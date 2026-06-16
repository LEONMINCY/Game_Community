<!-- 文件说明：views/chat.vue，旧版聊天页面，保留基础会话列表和单聊窗口展示。 -->
<template>
  <div style="height: 500px">
    <el-empty v-if="chats.length === 0" />
    <div class="chat-component" v-else>
      <div class="chat-list">
        <div v-for="(chat, index) in chats" :key="index" @click="setCurrentChat(index)" :class="{ active: currentChat === index, 'chat-unread': chat.unread }" :data-unread="chat.unread">
          <div class="chat-avatar">< img :src="chat.avatar" alt="avatar" /></div>
          <div class="chat-info">
            <div class="chat-name">{{ chat.name }}</div>
            <!--            <div class="chat-status" :class="{ online: chat.isOnline, offline: !chat.isOnline }">-->
            <!--              {{ chat.isOnline ? '在线' : '离线' }}-->
            <!--            </div>-->
            <div class="chat-message">{{ chat.messages && chat.messages.length > 0 ? chat.messages[chat.messages.length - 1].text : '' }}</div>
          </div>
        </div>
      </div>
      <div class="chat-window">
        <div class="chat-header">
          <div class="chat-avatar">< img :src="chats[currentChat].avatar" alt="avatar" /></div>
          <div class="chat-name">{{ chats[currentChat].name }}</div>
        </div>
        <div class="chat-body" ref="chatBox" v-if="chats[currentChat].messages.length > 0">
          <div v-for="(message, index) in chats[currentChat].messages" :key="index" class="chat-message-item" :class="{ 'is-me': message.userId === userId }">
            <div class="chat-message-avatar">< img :src="message.avatar" alt="avatar" /></div>
            <div class="chat-message-content">
              <div class="chat-message-text">{{ message.text }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else />
        <div class="chat-footer">
          <input v-model="newMessage" @keyup.enter="sendMessage" placeholder="Type a message..." />
        </div>
      </div>
    </div>
  </div>
</template>
<script >
export default {
  name: 'ChatList',
  data() {
    return {
      ws: null,
      newMessage: '',
      onlineUser: [],
    }
  },
  computed: {
    ...mapGetters(['userId', 'username', 'userAvatar', 'roleId']),
    ...mapState('chat', ['chats', 'currentChat']),
  },
  created() {
    this.ws = new WebSocket('ws://localhost:8089/ws?userId=' + this.userId)
    // 建立连接
    this.ws.onopen = () => {
      console.log('WebSocket连接成功')
    }
    // 接收消息
    this.ws.onmessage = (event) => {
      if (event.data === 'false') {
        this.$message.error('当前用户不在线，无法收到消息')
        return
      }
      const data = JSON.parse(event.data)

      const chat = this.chats.find((chat) => chat.userId === data.userId)
      // 如果消息列表存在
      if (chat) {
        const message = {
          userId: data.userId,
          name: data.name,
          avatar: data.avatar,
          text: data.text,
        }

        chat.messages.push(message)

        if (this.currentChat !== this.chats.indexOf(chat)) {
          chat.unread += 1
        }
      } else {
        // 不存在，新增
        const chat = {
          userId: data.userId,
          name: data.name,
          avatar: data.avatar,
          unread: 1,
          isOnline: true,
          messages: [
            {
              name: data.name,
              avatar: data.avatar,
              text: data.text,
              userId: data.userId,
            },
          ],
        }
        this.addChats(chat)
      }
    }
    // 连接错误
    this.ws.onerror = () => {
      this.$message.error('连接错误')
    }

  },
  destroyed() {
    this.ws.close()
  },
  methods: {
    ...mapMutations('chat', ['updateCurrentChat', 'updateUnread', 'addChat', 'addChats']),
    setCurrentChat(index) {
      this.updateCurrentChat(index)
      this.updateUnread(index)
    },
    sendMessage() {
      if (this.newMessage.trim() !== '') {
        const message = {
          name: this.username,
          avatar: this.userAvatar,
          userId: this.userId,
          text: this.newMessage.trim(),
          toUserId: this.chats[this.currentChat].userId,
        }
        this.addChat(message)
        this.ws.send(JSON.stringify(message))
        this.newMessage = ''
      }
      this.$nextTick(() => {
        this.$refs.chatBox.scrollTop = this.$refs.chatBox.scrollHeight
      })
    },
    updateOnlineUser() {
      getRequest('/user/online/count').then(res => {
        this.onlineUser.forEach(item => {
          if (res.info.contain(item.userId)) {
            item.isOnline = true
          }
        })
      })
    }
  }
}
</script>
