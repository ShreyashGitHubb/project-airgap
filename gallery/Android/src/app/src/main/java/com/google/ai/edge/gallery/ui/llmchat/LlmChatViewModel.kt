/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.llmchat

import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.proto.ChatMessageProto
import com.google.ai.edge.gallery.proto.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "AGLlmChatViewModel"

@OptIn(ExperimentalApi::class)
open class LlmChatViewModelBase(
  val dataStoreRepository: DataStoreRepository
) : ChatViewModel() {
  private var currentSessionId: String = UUID.randomUUID().toString()
  private var currentModelName: String = ""
  private var currentTaskId: String = ""

  private val _currentMedicalSkill = MutableStateFlow(MedicalSkillsProvider.getDefaultSkill())
  val currentMedicalSkill = _currentMedicalSkill.asStateFlow()

  fun setMedicalSkill(
    skill: MedicalSkill,
    model: Model,
    task: Task,
    supportImage: Boolean = false,
    supportAudio: Boolean = false
  ) {
    _currentMedicalSkill.value = skill
    currentModelName = model.name
    currentTaskId = task.id
    resetSession(
      task = task,
      model = model,
      systemInstruction = Contents.of(listOf(com.google.ai.edge.litertlm.Content.Text(skill.systemPrompt))),
      supportImage = supportImage,
      supportAudio = supportAudio
    )
  }

  fun loadSession(session: ChatSession, model: Model, task: Task) {
    currentSessionId = session.id
    currentModelName = session.modelName
    currentTaskId = session.taskId
    clearAllMessages(model)
    
    // Map proto messages to UI messages
    session.messagesList.forEach { msg ->
      val chatSide = when(msg.side) {
        "USER" -> ChatSide.USER
        "AGENT" -> ChatSide.AGENT
        else -> ChatSide.SYSTEM
      }
      
      val chatMessage = when(msg.type) {
        "THINKING" -> ChatMessageThinking(content = msg.content, inProgress = false, side = chatSide, accelerator = msg.accelerator)
        else -> ChatMessageText(content = msg.content, side = chatSide, latencyMs = msg.latencyMs, accelerator = msg.accelerator)
      }
      addMessage(model, chatMessage)
    }
  }

  private fun saveCurrentSession(model: Model) {
    val messages = uiState.value.messagesByModel[model.name] ?: return
    val protoMessages = messages.mapNotNull { msg ->
      val content = when(msg) {
        is ChatMessageText -> msg.content
        is ChatMessageThinking -> msg.content
        else -> null
      } ?: return@mapNotNull null
      
      ChatMessageProto.newBuilder()
        .setSide(msg.side.name)
        .setType(msg.type.name)
        .setContent(content)
        .setTimestamp(System.currentTimeMillis())
        .setLatencyMs(msg.latencyMs)
        .setAccelerator(msg.accelerator)
        .build()
    }

    if (protoMessages.isEmpty()) return

    val title = protoMessages.firstOrNull { it.side == "USER" }?.content?.take(30) ?: "New Analysis"
    
    val session = ChatSession.newBuilder()
      .setId(currentSessionId)
      .setTitle(title)
      .setTimestamp(System.currentTimeMillis())
      .addAllMessages(protoMessages)
      .setModelName(currentModelName)
      .setTaskId(currentTaskId)
      .build()
    
    dataStoreRepository.saveChatSession(session)
  }

  fun generateResponse(
    model: Model,
    input: String,
    images: List<Bitmap> = listOf(),
    audioMessages: List<ChatMessageAudioClip> = listOf(),
    onFirstToken: (Model) -> Unit = {},
    onDone: () -> Unit = {},
    onError: (String) -> Unit,
    allowThinking: Boolean = false,
  ) {
    val enhancedInput = if (images.size > 1 && _currentMedicalSkill.value.id.isNotEmpty()) {
      "[MULTIMODAL COMPARATIVE ANALYSIS REQUEST] Comparing ${images.size} provided images for evolutionary changes or bilateral symmetry abnormalities.\n\n$input"
    } else {
      input
    }

    val accelerator = model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = "")
    viewModelScope.launch(Dispatchers.Default) {
      setInProgress(true)
      setPreparing(true)

      // Loading.
      val progressLabel = if (_currentMedicalSkill.value.id.isNotEmpty()) "Processing Locally..." else ""
      addMessage(
        model = model,
        message = ChatMessageLoading(accelerator = accelerator, extraProgressLabel = progressLabel)
      )
      saveCurrentSession(model)

      // Wait for instance to be initialized.
      while (model.instance == null) {
        delay(100)
      }
      delay(500)

      // Run inference.
      val audioClips: MutableList<ByteArray> = mutableListOf()
      for (audioMessage in audioMessages) {
        audioClips.add(audioMessage.genByteArrayForWav())
      }

      var firstRun = true
      val start = System.currentTimeMillis()

      try {
        val resultListener: (String, Boolean, String?) -> Unit =
          { partialResult, done, partialThinkingResult ->
            if (partialResult.startsWith("<ctrl")) {
              // Do nothing. Ignore control tokens.
            } else {
              // Remove the last message if it is a "loading" message.
              // This will only be done once.
              val lastMessage = getLastMessage(model = model)
              val wasLoading = lastMessage?.type == ChatMessageType.LOADING
              if (wasLoading) {
                removeLastMessage(model = model)
              }

              val thinkingText = partialThinkingResult
              val isThinking = thinkingText != null && thinkingText.isNotEmpty()
              var currentLastMessage = getLastMessage(model = model)

              // If thinking is enabled, add a thinking message.
              if (isThinking) {
                if (currentLastMessage?.type != ChatMessageType.THINKING) {
                  addMessage(
                    model = model,
                    message =
                      ChatMessageThinking(
                        content = "",
                        inProgress = true,
                        side = ChatSide.AGENT,
                        accelerator = accelerator,
                        hideSenderLabel =
                          currentLastMessage?.type == ChatMessageType.COLLAPSABLE_PROGRESS_PANEL,
                      ),
                  )
                }
                updateLastThinkingMessageContentIncrementally(
                  model = model,
                  partialContent = thinkingText!!,
                )
              } else {
                if (currentLastMessage?.type == ChatMessageType.THINKING) {
                  val thinkingMsg = currentLastMessage as ChatMessageThinking
                  if (thinkingMsg.inProgress) {
                    replaceLastMessage(
                      model = model,
                      message =
                        ChatMessageThinking(
                          content = thinkingMsg.content,
                          inProgress = false,
                          side = thinkingMsg.side,
                          accelerator = thinkingMsg.accelerator,
                          hideSenderLabel = thinkingMsg.hideSenderLabel,
                        ),
                      type = ChatMessageType.THINKING,
                    )
                  }
                }
                currentLastMessage = getLastMessage(model = model)
                if (
                  currentLastMessage?.type != ChatMessageType.TEXT ||
                    currentLastMessage.side != ChatSide.AGENT
                ) {
                  // Add an empty message that will receive streaming results.
                  addMessage(
                    model = model,
                    message =
                      ChatMessageText(
                        content = "",
                        side = ChatSide.AGENT,
                        accelerator = accelerator,
                        hideSenderLabel =
                          currentLastMessage?.type == ChatMessageType.COLLAPSABLE_PROGRESS_PANEL ||
                            currentLastMessage?.type == ChatMessageType.THINKING,
                      ),
                  )
                }

                // Incrementally update the streamed partial results.
                val latencyMs: Long = if (done) System.currentTimeMillis() - start else -1
                if (partialResult.isNotEmpty() || wasLoading || done) {
                  updateLastTextMessageContentIncrementally(
                    model = model,
                    partialContent = partialResult,
                    latencyMs = latencyMs.toFloat(),
                  )
                }
              }

              if (firstRun) {
                firstRun = false
                setPreparing(false)
                onFirstToken(model)
              }

              if (done) {
                val finalLastMessage = getLastMessage(model = model)
                if (finalLastMessage?.type == ChatMessageType.THINKING) {
                  val thinkingMsg = finalLastMessage as ChatMessageThinking
                  if (thinkingMsg.inProgress) {
                    replaceLastMessage(
                      model = model,
                      message =
                        ChatMessageThinking(
                          content = thinkingMsg.content,
                          inProgress = false,
                          side = thinkingMsg.side,
                          accelerator = thinkingMsg.accelerator,
                          hideSenderLabel = thinkingMsg.hideSenderLabel,
                        ),
                      type = ChatMessageType.THINKING,
                    )
                  }
                }
                setInProgress(false)
                saveCurrentSession(model)
                onDone()
              }
            }
          }

        val cleanUpListener: () -> Unit = {
          setInProgress(false)
          setPreparing(false)
        }

        val errorListener: (String) -> Unit = { message ->
          Log.e(TAG, "Error occurred while running inference")
          setInProgress(false)
          setPreparing(false)
          onError(message)
        }

        val enableThinking =
          allowThinking &&
            model.getBooleanConfigValue(key = ConfigKeys.ENABLE_THINKING, defaultValue = false)
        val extraContext = if (enableThinking) mapOf("enable_thinking" to "true") else null

        model.runtimeHelper.runInference(
          model = model,
          input = enhancedInput,
          images = images,
          audioClips = audioClips,
          resultListener = resultListener,
          cleanUpListener = cleanUpListener,
          onError = errorListener,
          coroutineScope = viewModelScope,
          extraContext = extraContext,
        )
      } catch (e: Exception) {
        Log.e(TAG, "Error occurred while running inference", e)
        setInProgress(false)
        setPreparing(false)
        onError(e.message ?: "")
      }
    }
  }

  fun stopResponse(model: Model) {
    Log.d(TAG, "Stopping response for model ${model.name}...")
    if (getLastMessage(model = model) is ChatMessageLoading) {
      removeLastMessage(model = model)
    }
    setInProgress(false)
    model.runtimeHelper.stopResponse(model)
    Log.d(TAG, "Done stopping response")
  }

  fun resetSession(
    task: Task,
    model: Model,
    systemInstruction: Contents? = null,
    tools: List<ToolProvider> = listOf(),
    supportImage: Boolean = false,
    supportAudio: Boolean = false,
    onDone: () -> Unit = {},
    enableConversationConstrainedDecoding: Boolean = false,
  ) {
    viewModelScope.launch(Dispatchers.Default) {
      setIsResettingSession(true)
      clearAllMessages(model = model)
      stopResponse(model = model)
      currentSessionId = UUID.randomUUID().toString()

      while (true) {
        try {
          model.runtimeHelper.resetConversation(
            model = model,
            supportImage = supportImage,
            supportAudio = supportAudio,
            systemInstruction = systemInstruction,
            tools = tools,
            enableConversationConstrainedDecoding = enableConversationConstrainedDecoding,
          )
          break
        } catch (e: Exception) {
          Log.d(TAG, "Failed to reset session. Trying again")
        }
        delay(200)
      }
      setIsResettingSession(false)
      onDone()
    }
  }

  fun runAgain(
    model: Model,
    message: ChatMessageText,
    onError: (String) -> Unit,
    allowThinking: Boolean = false,
  ) {
    viewModelScope.launch(Dispatchers.Default) {
      // Wait for model to be initialized.
      while (model.instance == null) {
        delay(100)
      }

      // Clone the clicked message and add it.
      addMessage(model = model, message = message.clone())

      // Run inference.
      generateResponse(
        model = model,
        input = message.content,
        onError = onError,
        allowThinking = allowThinking,
      )
    }
  }

  fun handleError(
    context: Context,
    task: Task,
    model: Model,
    modelManagerViewModel: ModelManagerViewModel,
    errorMessage: String,
  ) {
    // Remove the "loading" message.
    if (getLastMessage(model = model) is ChatMessageLoading) {
      removeLastMessage(model = model)
    }

    // Show error message.
    addMessage(model = model, message = ChatMessageError(content = errorMessage))

    // Clean up and re-initialize.
    viewModelScope.launch(Dispatchers.Default) {
      modelManagerViewModel.cleanupModel(
        context = context,
        task = task,
        model = model,
        onDone = {
          modelManagerViewModel.initializeModel(context = context, task = task, model = model)

          // Add a warning message for re-initializing the session.
          addMessage(
            model = model,
            message = ChatMessageWarning(content = "Session re-initialized"),
          )
        },
      )
    }
  }
}

@HiltViewModel class LlmChatViewModel @Inject constructor(
  dataStoreRepository: DataStoreRepository
) : LlmChatViewModelBase(dataStoreRepository)

@HiltViewModel class LlmAskImageViewModel @Inject constructor(
  dataStoreRepository: DataStoreRepository
) : LlmChatViewModelBase(dataStoreRepository)

@HiltViewModel class LlmAskAudioViewModel @Inject constructor(
  dataStoreRepository: DataStoreRepository
) : LlmChatViewModelBase(dataStoreRepository)
