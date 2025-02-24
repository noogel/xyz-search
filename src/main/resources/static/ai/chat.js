class ChatUI {
    constructor() {
        this.messageContainer = document.getElementById('messageContainer');
        this.userInput = document.getElementById('userInput');
        this.sendButton = document.getElementById('sendButton');
        
        this.initializeEventListeners();
        this.setupMarkdown();
    }

    setupMarkdown() {
        marked.setOptions({
            highlight: function(code, lang) {
                return hljs.highlightAuto(code).value;
            },
            breaks: true
        });
    }

    initializeEventListeners() {
        this.sendButton.addEventListener('click', () => this.sendMessage());
        this.userInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
    }

    createMessageElement(content, isUser = false) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isUser ? 'user-message' : 'ai-message'}`;
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        
        if (isUser) {
            contentDiv.textContent = content;
        } else {
            contentDiv.innerHTML = marked.parse(content);
            // 代码高亮
            contentDiv.querySelectorAll('pre code').forEach((block) => {
                hljs.highlightBlock(block);
            });
        }
        
        messageDiv.appendChild(contentDiv);
        return messageDiv;
    }

    appendMessage(content, isUser = false) {
        const messageElement = this.createMessageElement(content, isUser);
        this.messageContainer.appendChild(messageElement);
        this.scrollToBottom();
    }

    scrollToBottom() {
        this.messageContainer.scrollTop = this.messageContainer.scrollHeight;
    }

    async sendMessage() {
        const message = this.userInput.value.trim();
        if (!message) return;

        // 禁用输入
        this.userInput.value = '';
        this.sendButton.disabled = true;
        this.userInput.disabled = true;

        // 显示用户消息
        this.appendMessage(message, true);

        try {
            const eventSource = new EventSource(`/ai/chat/stream?message=${encodeURIComponent(message)}`);
            let aiResponse = '';

            eventSource.onmessage = (event) => {
                const chunk = event.data;
                const data = JSON.parse(chunk);
                if (data.content === '') {
                    eventSource.close();
                    this.appendMessage(aiResponse);
                } else {
                    aiResponse += data.content;
                    // 更新最后一条消息的内容
                    const lastMessage = this.messageContainer.lastElementChild;
                    if (lastMessage && lastMessage.classList.contains('ai-message')) {
                        lastMessage.querySelector('.message-content').innerHTML = marked.parse(aiResponse);
                    } else {
                        this.appendMessage(aiResponse);
                    }
                }
                this.scrollToBottom();
            };

            eventSource.onerror = (error) => {
                console.error('SSE Error:', error);
                eventSource.close();
                this.appendMessage('连接错误，请重试', false);
            };
        } catch (error) {
            console.error('Error:', error);
            this.appendMessage('发送消息失败，请重试', false);
        } finally {
            // 恢复输入
            this.sendButton.disabled = false;
            this.userInput.disabled = false;
            this.userInput.focus();
        }
    }
}

// 初始化聊天界面
const chatUI = new ChatUI();