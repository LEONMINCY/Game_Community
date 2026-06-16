<!-- 文件说明：views/userinfo/index.vue，个人主页页面，展示资料、动态、拥有游戏、关注粉丝和收藏获赞统计。 -->
<template>
  <div class="app-container">
    <div class="backWrapper">
      <span  class="back" @click="goBack">&lt; 返回</span>
    </div>

    <div class="profile-more" v-if="!hasMyself">
      <el-dropdown trigger="click" @command="handleProfileCommand">
        <button class="more-button" type="button">
          <el-icon><MoreFilled /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="report">举报</el-dropdown-item>
            <el-dropdown-item command="block" divided>拉黑</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="userinfoWrapper">
      <div class="left">
        <el-avatar @mouseenter="showAvatarMask = true" style="cursor: pointer" :size="90" :src="avatar" />
        <el-upload
            class="avatar-uploader"
            action="/api/file/upload"
            :headers="headers"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
          <div class="avatarMask" v-if="showAvatarMask && hasMyself" @mouseleave="showAvatarMask = false" >
            <el-icon size="25" color="#fff"><EditPen /></el-icon>
          </div>
        </el-upload>
      </div>
      <div class="mid">
        <div class="name">
          <el-input ref="usernameInputRef" v-if="hasEditUsername" style="width: 100px" maxlength="10" @blur="editUsernameBlur()" v-model="nickname"  />
          <div v-else class="text">
            {{ nickname }}
          </div>

          <div  v-if="!hasMyself" class="leave">Lv{{ leave }}</div>
          <el-icon style="margin-left: 8px;cursor: pointer; transform: translateY(2px)" size="18" v-if="hasMyself" @click="editUsername()"><EditPen /></el-icon>
        </div>
        <div class="signature-text">{{ signatureText }}</div>
        <div class="leaveWrapper" v-if="hasMyself" >
          <div class="leave">Lv{{ leave }}</div>
          <el-progress  style="margin-left: 40px" :percentage="percentage" :text-inside="true" :stroke-width="18"/>
        </div>
      </div>
      <div class="right" v-if="!hasMyself">
        <el-button v-if="!isFollow" type="primary" size="small" icon="Plus" @click="goAndPayAttention">关注</el-button>
        <el-button v-else type="danger" size="small" icon="Delete" @click="cancelFollow(userId)">取消关注</el-button>
        <el-button type="primary" size="small" icon="ChatDotRound" @click="openAPrivateMessage">私信</el-button>
      </div>
      <div class="right" v-else>
        <el-button type="primary" v-if="!hasCheck" @click="userClockIn()">每日签到</el-button>
        <el-button :icon="Check" v-else disabled  type="success" >已签到</el-button>
      </div>

    </div>

    <div class="statistics">
      <div class="statisticsItem" @click="openUserInfoDialog">
        <div class="num">个人资料</div>
        <div class="numTitle" >{{ hasMyself ? '编辑' : '查看' }}</div>
      </div>
      <div class="statisticsItem" @click="openDailyTaskDialog" v-if="hasMyself">
        <div class="num">每日任务</div>
        <div class="numTitle">经验</div>
      </div>
      <div class="statisticsItem" @click="showFollowing">
        <div class="num">{{ userInfo.followCount ?? '已隐藏' }}</div>
        <div class="numTitle">关注</div>
      </div>
      <div class="statisticsItem" @click="showFollowers">
        <div class="num">{{ userInfo.fansCount ?? '已隐藏' }}</div>
        <div class="numTitle">粉丝</div>
      </div>
      <div class="statisticsItem" :class="{ disabled: !hasMyself }" @click="hasMyself && showFavorites()">
        <div class="num">{{ displayFavoriteCount }}</div>
        <div class="numTitle">{{ favoriteTitle }}</div>
      </div>
    </div>

    <!-- 添加个人资料 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="个人资料"
      width="500px"
      :close-on-click-modal="false"
    >
      <div v-if="hasMyself" class="dialog-content">
        <div class="info-item">
          <span class="label">性别</span>
          <el-radio-group v-model="userInfoForm.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
            <el-radio label="不方便透露">不方便透露</el-radio>
          </el-radio-group>
        </div>
        <div class="info-item">
          <span class="label">手机号</span>
          <el-input v-model="userInfoForm.phone" placeholder="请输入手机号" />
        </div>
        <div class="info-item">
          <span class="label">邮箱</span>
          <el-input v-model="userInfoForm.email" placeholder="用于找回密码，可选填" />
        </div>
        <div class="info-item">
          <span class="label">地址</span>
          <el-input v-model="userInfoForm.address" placeholder="请输入地址" />
        </div>
        <div class="info-item">
          <span class="label">年龄</span>
          <el-input-number v-model="userInfoForm.age" :min="1" :max="120" />
        </div>
        <div class="info-item signature-item">
          <span class="label">个性签名</span>
          <el-input
            v-model="userInfoForm.signature"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
            placeholder="写一句个性签名"
          />
        </div>
      </div>
      <div v-else class="profile-view-content">
        <div v-if="profileHiddenForViewer" class="privacy-notice">
          对方已设置个人资料隐私
        </div>
        <template v-else>
          <div class="profile-view-row">
            <span>性别</span>
            <strong>{{ profileDisplayValue(userInfo.gender) }}</strong>
          </div>
          <div class="profile-view-row">
            <span>手机号</span>
            <strong>{{ profileDisplayValue(userInfo.phone) }}</strong>
          </div>
          <div class="profile-view-row">
            <span>地址</span>
            <strong>{{ profileDisplayValue(userInfo.address) }}</strong>
          </div>
          <div class="profile-view-row">
            <span>年龄</span>
            <strong>{{ profileDisplayValue(userInfo.age) }}</strong>
          </div>
          <div class="profile-view-row profile-signature-row">
            <span>个性签名</span>
            <strong>{{ profileDisplayValue(userInfo.signature, 'TA还没有设置个性签名') }}</strong>
          </div>
        </template>
      </div>
      <template #footer v-if="hasMyself">
        <span class="dialog-footer">
          <el-button v-if="hasMyself" @click="openPasswordDialog">修改密码</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUserInfo" v-if="userStore.userInfo.userId == userInfoForm.userId">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dailyTaskDialogVisible"
      title="每日任务"
      width="620px"
      :close-on-click-modal="false"
    >
      <div class="daily-task-summary">
        <div>
          <strong>{{ dailyTaskSummary.totalExp || 0 }}</strong>
          <span>/ {{ dailyTaskSummary.dailyLimit || 120 }} 今日经验</span>
        </div>
        <el-progress :percentage="dailyTaskPercent" :stroke-width="12" />
      </div>
      <div class="daily-task-list">
        <div v-for="task in dailyTaskSummary.tasks" :key="task.type" class="daily-task-card">
          <div class="task-main">
            <div class="task-title">{{ task.title }}</div>
            <div class="task-desc">{{ task.desc }}</div>
          </div>
          <div class="task-progress">
            <div>{{ task.count }}/{{ task.maxCount }}</div>
            <el-progress :percentage="taskPercent(task)" :stroke-width="8" />
            <span>+{{ task.exp }}/{{ task.maxExp }} EXP</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="profileReportDialogVisible"
      title="举报用户"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="被举报用户">
          <span>{{ nickname || '-' }}</span>
        </el-form-item>
        <el-form-item label="举报原因" required>
          <el-input
            v-model="profileReportForm.reason"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="请说明举报原因"
          />
        </el-form-item>
        <el-form-item label="文字证据">
          <el-input
            v-model="profileReportForm.evidenceText"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="可以补充聊天内容、事件经过等文字证据"
          />
        </el-form-item>
        <el-form-item label="图片证据">
          <el-upload
            v-model:file-list="profileReportFileList"
            action="/api/file/upload"
            :headers="headers"
            list-type="picture-card"
            accept="image/*"
            :limit="6"
            :on-success="handleReportImageSuccess"
            :on-remove="handleReportImageRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileReportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProfileReport">提交举报</el-button>
      </template>
    </el-dialog>

    <!-- 添加修改密码的 Dialog -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <div class="info-item">
          <span class="label">原密码</span>
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </div>
        <div class="info-item">
          <span class="label">新密码</span>
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </div>
        <div class="info-item">
          <span class="label">确认密码</span>
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePassword">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 添加收藏列表的 Dialog -->
    <el-dialog
      v-model="favoritesDialogVisible"
      title="我的收藏"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="favorites-content">
        <el-input
          v-model="favoritesKeyword"
          clearable
          :placeholder="favoriteTab === 'posts' ? '搜索收藏的帖子标题、作者或游戏' : '搜索收藏的新闻标题或内容'"
          class="list-search"
        />
        <el-tabs v-model="favoriteTab">
          <el-tab-pane label="收藏的帖子" name="posts">
            <div v-if="filteredFavoritesList.length === 0" class="empty-state">
              <el-empty description="暂无收藏的帖子" />
            </div>
            <div v-else class="favorites-list">
              <div v-for="item in filteredFavoritesList" :key="item.id" class="favorite-item" @click="findOutMore(item.id)">
                <div class="favorite-header">
                  <div class="user-info">
                    <el-avatar :size="30" :src="item.userAvatar" />
                    <span class="username">{{ item.nickname }}</span>
                    <span class="level">Lv{{ item.userLevel }}</span>
                  </div>
                  <div class="game-tag">{{ item.gameName }}</div>
                </div>
                <div class="favorite-title">{{ item.title }}</div>
                <div class="favorite-content" v-html="removeTags(item.content)"></div>
                <div class="favorite-footer">
                  <span class="time">{{ item.createTime }}</span>
                  <el-button
                    type="danger"
                    size="small"
                    @click.stop="cancelFavorite(item)"
                    :icon="Star"
                    v-if="userStore.userInfo.userId === userInfo.userId"
                  >取消收藏</el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="收藏的新闻" name="news">
            <div v-if="filteredNewsFavoritesList.length === 0" class="empty-state">
              <el-empty description="暂无收藏的新闻" />
            </div>
            <div v-else class="favorites-list">
              <div v-for="item in filteredNewsFavoritesList" :key="item.id" class="favorite-item news-favorite-item" @click="findNewsDetail(item.id)">
                <div class="news-favorite-main">
                  <div class="news-favorite-body">
                    <div class="favorite-title">{{ item.title }}</div>
                    <div class="favorite-content" v-html="removeTags(item.content || '')"></div>
                    <div class="favorite-footer">
                      <span class="time">{{ item.createTime }}</span>
                      <span class="news-actions">点赞 {{ item.likes || 0 }} · 收藏 {{ item.favorites || 0 }} · 转发 {{ item.shareCount || 0 }}</span>
                    </div>
                  </div>
                  <el-image v-if="item.coverImage" class="news-favorite-cover" :src="item.coverImage" fit="cover" />
                </div>
                <div class="favorite-footer">
                  <span></span>
                  <el-button
                    type="danger"
                    size="small"
                    @click.stop="cancelNewsFavorite(item)"
                    :icon="Star"
                  >取消收藏</el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- 添加关注列表的 Dialog -->
    <el-dialog
      v-model="followingDialogVisible"
      title="关注列表"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="user-list-content">
        <el-input
          v-model="followingKeyword"
          clearable
          placeholder="搜索关注用户"
          class="list-search"
        />
        <div v-if="filteredFollowingList.length === 0" class="empty-state">
          <el-empty description="暂无关注" />
        </div>
        <div v-else class="user-list">
          <div v-for="user in filteredFollowingList" :key="user.userId" class="user-item">
            <div class="user-info" @click="viewUsers(user.userId)">
              <el-avatar :size="40" :src="replaceURL(user.avatar)" />
              <div class="user-details">
                <span class="username">{{ user.nickname }}</span>
              </div>
            </div>
            <el-button
              type="danger"
              size="small"
              @click="cancelFollow(user.userId)"
            >取消关注</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 添加粉丝列表的 Dialog -->
    <el-dialog
      v-model="followersDialogVisible"
      title="粉丝列表"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="user-list-content">
        <el-input
          v-model="followersKeyword"
          clearable
          placeholder="搜索粉丝用户"
          class="list-search"
        />
        <div v-if="filteredFollowersList.length === 0" class="empty-state">
          <el-empty description="暂无粉丝" />
        </div>
        <div v-else class="user-list">
          <div v-for="user in filteredFollowersList" :key="user.userId" class="user-item">
            <div class="user-info" @click="viewUsers(user.userId)">
              <el-avatar :size="40" :src="replaceURL(user.avatar)" />
              <div class="user-details">
                <span class="username">{{ user.nickname }}</span>
              </div>
            </div>
            <el-button
              :type="user.isFollowed ? 'danger' : 'primary'"
              size="small"
              @click="user.isFollowed ? cancelFollow(user.userId) :  followUser(user.userId)"
            >{{ user.isFollowed ? '取消关注' : '关注' }}</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <div class="dynamic">
      <div class="profile-section-tabs">
        <span class="header" :class="{ active: activeProfileTab === 'dynamic' }" @click="selectProfileTab('dynamic')">动 态</span>
        <span class="header" :class="{ active: activeProfileTab === 'owned' }" @click="selectProfileTab('owned')">拥有游戏</span>
      </div>

      <div v-if="activeProfileTab === 'dynamic'" class="dynamicContent">
        <div class="listWrapper">
          <div class="listItem" v-for="(item, index) in forumList" :key="item.id + 'index'">
            <div class="userInfo" >
              <el-avatar style="cursor: pointer" :size="25" :src="item.userAvatar" />
              <div style="cursor: pointer"  class="name">{{ item.nickname }}</div>
              <div class="leave" style="font-size: 5px;cursor: pointer">Lv{{ item.userLevel }}</div>
            </div>
            <div @click="findOutMore(item.id)">
              <div class="titleWrapper">
                {{ item.title }}
              </div>
              <div class="contentWrapper" v-html="removeTags(item.content)">

              </div>
              <div class="imgWrapper">
                <div class="imgCard" v-for="(img, index2) in item.images" :key="index2 + 'img'">
                  <el-image style="width: 100%; height: 100%" :src="img" fit="contain" lazy >
                  </el-image>
                </div>
              </div>
              <div class="dynamicStats">
                <span>点赞 {{ item.likes || 0 }}</span>
                <span>收藏 {{ item.favorites || 0 }}</span>
                <span>评论 {{ item.commentCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="ownedContent">
        <div v-if="profileHiddenForViewer" class="owned-privacy">
          对方已设置个人资料隐私
        </div>
        <template v-else>
          <div class="owned-summary">
            <div class="owned-stat-card games">
              <strong>{{ ownedSummary.gameCount || 0 }}</strong>
              <span>拥有游戏</span>
            </div>
            <div class="owned-stat-card value">
              <strong>¥{{ ownedSummary.accountValue || 0 }}</strong>
              <span>账号价值</span>
            </div>
          </div>
          <div v-if="ownedGames.length === 0" class="owned-empty">暂无已购买游戏</div>
          <div v-else class="owned-list">
            <div v-for="game in ownedGames" :key="game.gameId" class="owned-game-card" @click="findGameDetail(game.gameId)">
              <img :src="game.icon" alt="" loading="lazy" decoding="async" @error="useOriginalImage($event, game.originalIcon)" />
              <div class="owned-game-info">
                <h3>{{ game.name }}</h3>
                <span>{{ game.developer || '未设置开发商' }}</span>
                <div class="owned-price-row">
                  <strong>¥{{ game.price || 0 }}</strong>
                  <em v-if="game.discount > 0">当前 -{{ game.discount }}%</em>
                  <i v-if="priceMarkText(game.priceMark)">{{ priceMarkText(game.priceMark) }}</i>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

    </div>

  </div>
</template>
<script setup>
import {
  Check,
  Delete,
  Edit,
  Message,
  MoreFilled,
  Plus,
  Search,
  Star,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, onMounted, reactive, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getRequest, postRequest, putRequest, deleteRequest } from "../../utils/http";
import { useUserStore } from '@/store'
import { replaceURL, thumbnailURL, removeTags } from '@/utils/tools'
import { requireLogin } from '@/utils/auth'
import { ElRadioGroup, ElRadio, ElInputNumber } from 'element-plus'
import { nextTick } from 'vue';
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const forumList = ref([])
const showAvatarMask = ref(false)
const hasCheck = ref(false)
const hasMyself = ref(true)
const userId = ref('')
const hasEditUsername = ref(false)
const nickname = ref('')
const avatar = ref('')
const signature = ref('')
const isFollow = ref(false)
const usernameInputRef = ref(null)
const headers = ref({
  Authorization: localStorage.getItem('token')
})
const userInfo = ref({})

const leave = ref(0)
const percentage = ref(0)

// 添加编辑状态和表单数据
const dialogVisible = ref(false)
const userInfoForm = reactive({
  gender: '',
  phone: '',
  email: '',
  address: '',
  age: null,
  signature: ''
})

const passwordDialogVisible = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const dailyTaskDialogVisible = ref(false)
const dailyTaskSummary = ref({
  totalExp: 0,
  dailyLimit: 120,
  tasks: []
})

const favoritesDialogVisible = ref(false)
const favoritesList = ref([])
const newsFavoritesList = ref([])
const favoritesKeyword = ref('')
const favoriteTab = ref('posts')

const followingDialogVisible = ref(false)
const followingList = ref([])
const followingKeyword = ref('')

const followersDialogVisible = ref(false)
const followersList = ref([])
const followersKeyword = ref('')
const profileReportDialogVisible = ref(false)
const profileReportFileList = ref([])
const profileReportImages = ref([])
const profileReportForm = reactive({
  reason: '',
  evidenceText: ''
})
const activeProfileTab = ref('dynamic')
const ownedSummary = ref({
  gameCount: 0,
  accountValue: 0
})
const ownedGames = ref([])
const ownedLoaded = ref(false)
const mobilePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const matchKeyword = (value, keyword) => String(value || '').toLowerCase().includes(keyword.trim().toLowerCase())

const filteredFavoritesList = computed(() => {
  const keyword = favoritesKeyword.value
  if (!keyword.trim()) {
    return favoritesList.value
  }
  return favoritesList.value.filter(item =>
    matchKeyword(item.title, keyword) ||
    matchKeyword(item.nickname, keyword) ||
    matchKeyword(item.gameName, keyword) ||
    matchKeyword(removeTags(item.content || ''), keyword)
  )
})

const filteredNewsFavoritesList = computed(() => {
  const keyword = favoritesKeyword.value
  if (!keyword.trim()) {
    return newsFavoritesList.value
  }
  return newsFavoritesList.value.filter(item =>
    matchKeyword(item.title, keyword) ||
    matchKeyword(removeTags(item.content || ''), keyword)
  )
})

const filteredFollowingList = computed(() => {
  const keyword = followingKeyword.value
  if (!keyword.trim()) {
    return followingList.value
  }
  return followingList.value.filter(user => matchKeyword(user.nickname, keyword))
})

const filteredFollowersList = computed(() => {
  const keyword = followersKeyword.value
  if (!keyword.trim()) {
    return followersList.value
  }
  return followersList.value.filter(user => matchKeyword(user.nickname, keyword))
})

const signatureText = computed(() => {
  if (signature.value && signature.value.trim()) {
    return signature.value
  }
  return hasMyself.value ? '还没有设置个性签名' : 'TA还没有设置个性签名'
})

const displayFavoriteCount = computed(() => {
  return hasMyself.value ? (userInfo.value.favoriteCount || 0) : (userInfo.value.favoriteLikeCount || 0)
})

const favoriteTitle = computed(() => {
  return hasMyself.value ? '收藏' : '收藏与获赞'
})

const profileHiddenForViewer = computed(() => {
  return !hasMyself.value && Boolean(userInfo.value.privacyProfile)
})

const profileDisplayValue = (value, emptyText = '未填写') => {
  if (value === null || value === undefined || value === '') {
    return emptyText
  }
  return value
}

const priceMarkText = (value) => {
  if (value === 'historical_low') return '史低'
  if (value === 'tie_historical_low') return '平史低'
  return ''
}

const useOriginalImage = (event, originalUrl) => {
  const image = event.target
  if (!image || !originalUrl || image.dataset.fallback === '1') {
    return
  }
  image.dataset.fallback = '1'
  image.src = originalUrl
}

const dailyTaskPercent = computed(() => {
  const total = Number(dailyTaskSummary.value.totalExp) || 0
  const limit = Number(dailyTaskSummary.value.dailyLimit) || 120
  return Math.min(100, Math.round((total / limit) * 100))
})

const taskPercent = (task) => {
  if (!task) return 0
  const countPercent = ((Number(task.count) || 0) / Math.max(1, Number(task.maxCount) || 1)) * 100
  const expPercent = ((Number(task.exp) || 0) / Math.max(1, Number(task.maxExp) || 1)) * 100
  return Math.min(100, Math.round(Math.max(countPercent, expPercent)))
}

const refreshProfileTarget = () => {
  const queryParams = route.query;
  if (queryParams.userId && queryParams.userId != userStore.userInfo.userId) {
    userId.value = queryParams.userId
    hasMyself.value = false
  } else {
    userId.value = sessionStorage.getItem('userId')
    hasMyself.value = true

  }
}

const reloadProfilePage = () => {
  refreshProfileTarget()
  getUserInfo()
  getForumList()
  if (activeProfileTab.value === 'owned') {
    getOwnedGames()
  }
}

onMounted(() => {
  reloadProfilePage()
})

watch(
  () => route.query.userId,
  () => {
    reloadProfilePage()
  }
)

const goAndPayAttention = () => {
  postRequest('/userRelation/follow/' + userId.value).then(res => {
    if (res.code === 200) {
      ElMessage.success('关注成功')
      isFollow.value = true
      getUserInfo()
    }
  })
}

const getForumList = () => {
  getRequest('/post/user/' + userId.value).then(res => {
    if (res.code === 200) {
      forumList.value = res.data.map(item => {
        return {
          ...item,
          images: JSON.parse(item.media) !== [] ? JSON.parse(item.media).map(img => replaceURL(img)) : [],
          userAvatar: replaceURL(item.userAvatar)
        }
      })
    }
  })
}

const getOwnedGames = () => {
  if (!userId.value || profileHiddenForViewer.value) {
    return
  }
  getRequest('/game-user/owned/summary/' + userId.value).then(res => {
    if (res.code === 200) {
      ownedSummary.value = {
        gameCount: res.data?.gameCount || 0,
        accountValue: res.data?.accountValue || 0
      }
      ownedGames.value = (res.data?.games || []).map(item => ({
        ...item,
        originalIcon: item.icon ? replaceURL(item.icon) : '',
        icon: item.icon ? thumbnailURL(replaceURL(item.icon), 280) : ''
      }))
      ownedLoaded.value = true
    }
  })
}

const goBack = () => {
  router.back()
}

const openAPrivateMessage = () => {
  if (!requireLogin(route.fullPath)) return
  router.push({
    path: '/chat',
    query: {
      userId: userId.value
    }
  })
}

const handleProfileCommand = (command) => {
  if (command === 'report') {
    openProfileReportDialog()
  }
  if (command === 'block') {
    blockProfileUser()
  }
}

const openProfileReportDialog = () => {
  if (!requireLogin(route.fullPath)) return
  profileReportForm.reason = ''
  profileReportForm.evidenceText = ''
  profileReportImages.value = []
  profileReportFileList.value = []
  profileReportDialogVisible.value = true
}

const blockProfileUser = () => {
  if (!requireLogin(route.fullPath)) return
  ElMessageBox.confirm(
    `确定要拉黑 ${nickname.value || '该用户'} 吗？拉黑后对方将无法再给你发送私信。`,
    '确认拉黑',
    {
      confirmButtonText: '确认拉黑',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    postRequest('/blacklist/block/' + userId.value).then(res => {
      if (res.code === 200) {
        ElMessage.success('已加入黑名单')
      } else {
        ElMessage.error(res.msg || '拉黑失败')
      }
    })
  }).catch(() => {})
}

const handleReportImageSuccess = (res) => {
  if (res.code === 200 && res.data) {
    profileReportImages.value.push(res.data)
  } else {
    ElMessage.error(res.msg || '图片上传失败')
  }
}

const handleReportImageRemove = (file) => {
  const target = file.response?.data || file.url
  profileReportImages.value = profileReportImages.value.filter(item => item !== target)
}

const submitProfileReport = () => {
  const reason = profileReportForm.reason.trim()
  if (!reason) {
    ElMessage.warning('请填写举报原因')
    return
  }
  postRequest('/report/add', {
    reportedId: Number(userId.value),
    reportType: 'USER',
    reason,
    evidenceText: profileReportForm.evidenceText.trim(),
    evidenceImages: JSON.stringify(profileReportImages.value)
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('举报已提交')
      profileReportDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '举报提交失败')
    }
  }).catch(() => {
    ElMessage.error('举报提交失败')
  })
}

const findOutMore = (id) => {
  router.push('/forum?id=' + id)
}

const findNewsDetail = (id) => {
  router.push('/news/detail?id=' + id)
}

const findGameDetail = (id) => {
  router.push({ path: '/gameDetail', query: { id } })
}

const selectProfileTab = (tab) => {
  activeProfileTab.value = tab
  if (tab === 'owned' && !ownedLoaded.value && !profileHiddenForViewer.value) {
    getOwnedGames()
  }
}

const editUsernameBlur = () => {
  hasEditUsername.value = false
  putUsername()
}

const editUsername = () => {
  hasEditUsername.value = true
  setTimeout(() => {
    usernameInputRef.value.focus()
  }, 100)
}

const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    putAvatar(res.data)
    avatar.value = replaceURL(res.data)
  }
}

const beforeAvatarUpload = (file) => {
  return file.type.startsWith('image/')
}




//接口
const getUserInfo = () => {
  getRequest('/user-info/get-real/' + userId.value).then(res => {
    if (res.code === 200) {
      userInfo.value = res.data
      nickname.value = res.data.nickname
      avatar.value = replaceURL(res.data.avatar)
      signature.value = res.data.signature || ''
      leave.value = res.data.ex2
      percentage.value = res.data.ex1
      hasCheck.value = !!res.data.hasCheckedToday
      isFollow.value = res.data.follow

      // 更新表单数据
      userInfoForm.gender = res.data.gender || ''
      userInfoForm.phone = res.data.phone || ''
      userInfoForm.email = res.data.email || ''
      userInfoForm.address = res.data.address || ''
      userInfoForm.age = res.data.age || null
      userInfoForm.signature = res.data.signature || ''
      if (activeProfileTab.value === 'owned' && !profileHiddenForViewer.value) {
        getOwnedGames()
      }
    }
  })
}
//用户打卡签到
const userClockIn = () => {
  putRequest('/user-info/clock').then(res => {
    if (res.code === 200) {
      ElMessage.success('签到成功')
      getUserInfo()
      hasCheck.value = true
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const putUsername = () => {
  putRequest('/user-info/update', {
    nickname: nickname.value,
    userId: userId.value
  }).then(res => {
    if (res.code === 200) {
      getUserInfo()
      userStore.userInfo.nickname = nickname.value
      ElMessage.success('修改成功')
    }
  })
}

const putAvatar = (avatar) => {
  putRequest('/user-info/update', {
    avatar: avatar,
    userId: userId.value
  })
}

const cancelFollow = (userId)=> {
  deleteRequest('/userRelation/unfollow/' + userId).then(res => {
    if (res.code === 200) {
      ElMessage.success('取消关注成功')
      if (String(userId) === String(userInfo.value.userId)) {
        isFollow.value = false
      }
      followingList.value = followingList.value.filter(user => String(user.userId) !== String(userId))
      followersList.value = followersList.value.map(user => {
        if (String(user.userId) === String(userId)) {
          return {
            ...user,
            isFollowed: false
          }
        }
        return user
      })
      if (hasMyself.value && userInfo.value.followCount > 0) {
        userInfo.value.followCount--
      }
      getUserInfo()
    }
  })
}

// 修改开始编辑方法为打开对话框方法
const openUserInfoDialog = () => {
  userInfoForm.userId = userInfo.value.userId
  userInfoForm.gender = userInfo.value.gender || ''
  userInfoForm.phone = userInfo.value.phone || ''
  userInfoForm.email = userInfo.value.email || ''
  userInfoForm.address = userInfo.value.address || ''
  userInfoForm.age = userInfo.value.age || null
  userInfoForm.signature = userInfo.value.signature || ''
  dialogVisible.value = true
}

// 修改保存方法
const saveUserInfo = () => {
  const phone = String(userInfoForm.phone || '').trim()
  if (phone && !mobilePattern.test(phone)) {
    ElMessage.warning('请输入合法的11位手机号')
    return
  }
  const email = String(userInfoForm.email || '').trim()
  if (email && !emailPattern.test(email)) {
    ElMessage.warning('请输入合法的邮箱地址')
    return
  }
  putRequest('/user-info/update', {
    userId: userId.value,
    gender: userInfoForm.gender,
    phone,
    email,
    address: userInfoForm.address,
    age: userInfoForm.age,
    signature: userInfoForm.signature
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      getUserInfo() // 重新获取用户信息
    }
  }).catch(() => {
    ElMessage.error('保存失败')
  })
}

// 打开修改密码对话框
const openDailyTaskDialog = () => {
  if (!hasMyself.value) {
    return
  }
  dailyTaskDialogVisible.value = true
  getRequest('/daily-task/summary').then(res => {
    if (res.code === 200) {
      dailyTaskSummary.value = res.data || { totalExp: 0, dailyLimit: 120, tasks: [] }
    }
  })
}

const openPasswordDialog = () => {
  if (!hasMyself.value) {
    return
  }
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

// 保存密码
const savePassword = () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整的密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能小于6位')
    return
  }

  postRequest('/update-password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      passwordDialogVisible.value = false
      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  }).catch(() => {
    ElMessage.error('密码修改失败')
  })
}

// 显示收藏列表
const showFavorites = () => {
  if (!hasMyself.value) {
    return
  }
  favoritesKeyword.value = ''
  favoriteTab.value = 'posts'
  favoritesDialogVisible.value = true
  getFavoritesList()
  getNewsFavoritesList()
}

// 获取收藏列表
const getFavoritesList = () => {
  getRequest('/post/favorites/'+userInfo.value.userId).then(res => {
    if (res.code === 200) {
      favoritesList.value = res.data.map(item => {
        return {
          ...item,
          userAvatar: replaceURL(item.userAvatar),
          images: JSON.parse(item.media || '[]').map(img => replaceURL(img))
        }
      })
    }
  })
}

// 取消收藏
const cancelFavorite = (item) => {
  postRequest('/post/unFavorite/' + item.id).then(res => {
    if (res.code === 200) {
      ElMessage.success('取消收藏成功')
      // 从列表中移除
      favoritesList.value = favoritesList.value.filter(i => i.id !== item.id)
      // 更新收藏数
      if (userInfo.value.favoriteCount > 0) {
        userInfo.value.favoriteCount--
      }
    }
  })
}

const showFollowing = () => {
  followingKeyword.value = ''
  getFollowingList()
  followingDialogVisible.value = true
}

const getFollowingList = () => {
  getRequest('/userRelation/following/' + userId.value).then(res => {
    if (res.code === 200) {
      followingList.value = res.data.map(item => {
        return {
          ...item,
          avatar: replaceURL(item.avatar)
        }
      })
    } else {
      ElMessage.warning(res.msg || '该用户已隐藏关注列表')
      followingDialogVisible.value = false
    }
  })
}

const cancelNewsFavorite = (item) => {
  postRequest('/news/unFavorite/' + item.id).then(res => {
    if (res.code === 200) {
      ElMessage.success('取消收藏成功')
      newsFavoritesList.value = newsFavoritesList.value.filter(i => i.id !== item.id)
    } else {
      ElMessage.error(res.msg || '取消收藏失败')
    }
  })
}

const parseNewsCover = (mediaJson) => {
  if (!mediaJson) return ''
  try {
    const parsed = JSON.parse(mediaJson)
    if (Array.isArray(parsed)) {
      return parsed[0] ? replaceURL(parsed[0]) : ''
    }
    return parsed ? replaceURL(parsed) : ''
  } catch (error) {
    return replaceURL(mediaJson)
  }
}

const getNewsFavoritesList = () => {
  getRequest('/news/favorites/' + userInfo.value.userId).then(res => {
    if (res.code === 200) {
      newsFavoritesList.value = (res.data || []).map(item => ({
        ...item,
        coverImage: parseNewsCover(item.mediaJson)
      }))
    }
  })
}

const showFollowers = () => {
  followersKeyword.value = ''
  getFollowersList()
  followersDialogVisible.value = true
}

const getFollowersList = () => {
  getRequest('/userRelation/followers/' + userId.value).then(res => {
    if (res.code === 200) {
      followersList.value = res.data.map(item => {
        return {
          ...item,
          avatar: replaceURL(item.avatar)
        }
      })
    } else {
      ElMessage.warning(res.msg || '该用户已隐藏粉丝列表')
      followersDialogVisible.value = false
    }
  })
}

const viewUsers = (userId) => {
  followingDialogVisible.value = false
  followersDialogVisible.value = false
  router.push({ path: '/userinfo', query: { userId } })
}

const followUser = (userId) => {
  postRequest('/userRelation/follow/' + userId).then(res => {
    if (res.code === 200) {
      ElMessage.success('关注成功')
      followersList.value = followersList.value.map(user => {
        if (String(user.userId) === String(userId)) {
          return {
            ...user,
            isFollowed: true
          }
        }
        return user
      })
      const followedUser = followersList.value.find(user => String(user.userId) === String(userId))
      if (followedUser && !followingList.value.some(user => String(user.userId) === String(userId))) {
        followingList.value.unshift({
          ...followedUser,
          isFollowed: true
        })
      }
      if (hasMyself.value) {
        userInfo.value.followCount = (userInfo.value.followCount || 0) + 1
      }
      getUserInfo()
    }
  })
}

</script>
<style lang="less" scoped>
.app-container {
  width: 100%;
  height: 100%;
  position: relative;
  box-sizing: border-box;
  .backWrapper {
    font-size: 14px;
    font-weight: 550;
    position: absolute;
    left: 15px;
    top: 15px;

    .back {
      background-color: #f7f8f9;
      padding: 5px 10px;
      cursor: pointer;
      border-radius: 5px;
    }

  }

  .profile-more {
    position: absolute;
    right: 18px;
    top: 16px;
    z-index: 5;

    .more-button {
      width: 36px;
      height: 36px;
      border: 0;
      border-radius: 50%;
      background: #f7f8f9;
      color: #606266;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #edf2f7;
        color: #303133;
      }
    }
  }

  .userinfoWrapper {
    padding: 44px 15px 6px 15px;
    display: flex;
    align-items: center;

    .left {
      position: relative;
      width: 72px;
      :deep(.el-avatar) {
        width: 72px !important;
        height: 72px !important;
      }
      .avatarMask {
        left: 0;
        top: 0;
        width: 72px;
        height: 72px;
        border-radius: 50%;
        position: absolute;
        background-color: rgba(0,0,0,0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;
      }
    }

    .right {
      width: 120px;
      display: flex;
      justify-content: end;
      transform: translateY(13px);
    }

    .mid {
      margin-left: 20px;
      flex: 1;
      box-sizing: border-box;


      .name {
        display: flex;
        align-items: center;
        font-size: 18px;
        font-weight: 700;
        margin-bottom: 6px;

        .leave {
          background-color: #329afe;
          color: #fff;
          font-size: 10px !important;
          margin-left: 8px;
          height: 12px;
          line-height: 12px;
          padding: 1px 2px;
          border-radius: 3px;
        }
      }

      .signature-text {
        max-width: 560px;
        margin: -2px 0 6px;
        color: #8a9199;
        font-size: 13px;
        line-height: 1.5;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .leaveWrapper {
        position: relative;
        .leave {
          position: absolute;
          left: 0;
          top: 0;
          color: #329afe;
          font-size: 13px;
          font-weight: 700;
        }
      }
    }
  }


  .statistics {
    width: 100%;
    display: flex;


      .statisticsItem {
        flex: 1;
        text-align: center;
        cursor: pointer;
        padding: 18px 0 16px;
      border-bottom: 4px solid #f7f8f9;

      .num {
        font-size: 18px;
        color: #14191e;
        font-weight: 550;
      }

      .numTitle {
        font-size: 14px;
        color: #c8cdd2;
      }

      &.disabled {
        cursor: default;
      }
    }
  }

  .dynamic {
    padding: 10px 15px 18px;

    .profile-section-tabs {
      display: flex;
      align-items: center;
      gap: 22px;
    }

    .header {
      padding: 4px 4px;
      box-sizing: border-box;
      font-size: 15px;
      font-weight: 550;
      color: #14191e;
      margin-bottom: 10px;
      border-bottom: 2px solid transparent;
      cursor: pointer;

      &.active {
        border-bottom-color: #4a4a4a;
      }
    }

    .dynamicContent {
      margin-top: 8px;
      width: 100%;
      min-height: 310px;
      height: calc(100vh - 320px);
      overflow-y: auto;
      position: static;
      -ms-overflow-style: none;
      overflow: -moz-scrollbars-none;

      &::-webkit-scrollbar {
        display: none;
      }

      .listWrapper {
        width: 100%;


        .listItem {
          width: 100%;
          margin-bottom: 10px;
          padding: 14px 16px;
          box-sizing: border-box;
          border: 1px solid #f0f2f5;
          border-radius: 8px;
          background: #fff;

          .userInfo {
            display: flex;
            align-items: center;

            .name {
              margin-left: 6px;
              color: rgb(100, 105, 110);
              font-size: 12px;
            }
            .leave {
              background-color: #329afe;
              color: #fff;
              font-size: 10px !important;
              margin-left: 8px;
              height: 12px;
              line-height: 12px;
              padding: 1px 2px;
              border-radius: 3px;

            }

          }

          .titleWrapper {
            margin-top: 8px;
            font-size: 17px;
            font-weight: 700;
          }

          .contentWrapper {
            font-size: 14px;
            margin-top: 6px;
            width: 100%;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 3;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .imgWrapper {
            margin-top: 10px;
            width: 100%;
            min-height: 140px;
            max-height: 220px;
            box-sizing: border-box;
            overflow: hidden;
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            grid-gap: 10px;


            .imgCard {
              height: 100%;
              border-radius: 10px;
              overflow: hidden;
            }
          }

          .dynamicStats {
            display: flex;
            align-items: center;
            gap: 16px;
            margin-top: 10px;
            color: #8a92a3;
            font-size: 13px;

            span {
              display: inline-flex;
              align-items: center;
              white-space: nowrap;
            }
          }
        }

      }
    }
  }
}

.dialog-content {
  padding: 20px;

  .info-item {
    display: flex;
    align-items: center;
    margin-bottom: 20px;

    .label {
      width: 80px;
      color: #666;
      font-size: 14px;
    }

    :deep(.el-input),
    :deep(.el-input-number) {
      width: 300px;
    }

    :deep(.el-radio-group) {
      margin-left: 0;
    }

    &.signature-item {
      align-items: flex-start;

      :deep(.el-textarea) {
        width: 300px;
      }
    }

    .ownedContent {
      margin-top: 12px;
      min-height: 310px;
      height: calc(100vh - 360px);
      overflow-y: auto;
    }

    .owned-privacy,
    .owned-empty {
      min-height: 180px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #f0f2f5;
      border-radius: 8px;
      color: #909399;
      background: #fafbfc;
    }

    .owned-summary {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 12px;

      div {
        padding: 16px;
        border: 1px solid #f0f2f5;
        border-radius: 8px;
        background: #fff;
      }

      strong {
        display: block;
        color: #14191e;
        font-size: 24px;
        line-height: 1.2;
      }

      span {
        color: #909399;
        font-size: 13px;
      }
    }

    .owned-list {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    .owned-game-card {
      display: flex;
      gap: 12px;
      min-width: 0;
      padding: 12px;
      border: 1px solid #f0f2f5;
      border-radius: 8px;
      background: #fff;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
      }

      img {
        width: 112px;
        height: 64px;
        object-fit: cover;
        border-radius: 6px;
        background: #f2f4f7;
        flex-shrink: 0;
      }
    }

    .owned-game-info {
      min-width: 0;

      h3 {
        margin: 0 0 6px;
        color: #14191e;
        font-size: 15px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      span {
        display: block;
        color: #7a828c;
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .owned-price-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 8px;

      strong {
        color: #f56c6c;
      }

      em,
      i {
        font-style: normal;
        font-size: 12px;
        border-radius: 4px;
        padding: 1px 6px;
      }

      em {
        color: #f56c6c;
        background: #fff5f5;
      }

      i {
        color: #409eff;
        background: #ecf5ff;
      }
    }

    .ownedContent {
      margin-top: 8px;
      min-height: 310px;
      height: calc(100vh - 320px);
      overflow-y: auto;
      padding-right: 2px;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-thumb {
        border-radius: 999px;
        background: #d9dee7;
      }
    }

    .owned-privacy,
    .owned-empty {
      min-height: 180px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #eef1f6;
      border-radius: 10px;
      color: #909399;
      background: #fafbfc;
    }

    .owned-summary {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 14px;
    }

    .owned-stat-card {
      position: relative;
      overflow: hidden;
      padding: 16px 18px;
      border-radius: 12px;
      border: 1px solid transparent;

      &::after {
        content: '';
        position: absolute;
        right: -18px;
        top: -28px;
        width: 92px;
        height: 92px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.42);
      }

      &.games {
        background: linear-gradient(135deg, #ecf5ff 0%, #f7fbff 100%);
        border-color: #d8ebff;
      }

      &.value {
        background: linear-gradient(135deg, #fff3e6 0%, #fffaf3 100%);
        border-color: #ffe2bf;
      }

      strong {
        display: block;
        color: #14191e;
        font-size: 26px;
        line-height: 1.1;
        letter-spacing: 0;
      }

      span {
        display: block;
        margin-top: 6px;
        color: #68717d;
        font-size: 13px;
      }
    }

    .owned-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
      gap: 12px;
    }

    .owned-game-card {
      display: flex;
      gap: 10px;
      min-width: 0;
      padding: 10px;
      border: 1px solid #eef1f6;
      border-radius: 10px;
      background: #fff;
      cursor: pointer;
      transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

      &:hover {
        transform: translateY(-1px);
        border-color: #dce8f8;
        box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
      }

      img {
        width: 92px;
        height: 58px;
        object-fit: cover;
        border-radius: 8px;
        background: #f2f4f7;
        flex-shrink: 0;
      }
    }

    .owned-game-info {
      min-width: 0;
      flex: 1;

      h3 {
        margin: 0 0 5px;
        color: #14191e;
        font-size: 14px;
        line-height: 1.3;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      span {
        display: block;
        color: #7a828c;
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .owned-price-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 5px;
      margin-top: 6px;

      strong {
        color: #f56c6c;
        font-size: 14px;
      }

      em,
      i {
        font-style: normal;
        font-size: 11px;
        border-radius: 4px;
        padding: 1px 5px;
      }

      em {
        color: #f56c6c;
        background: #fff5f5;
      }

      i {
        color: #409eff;
        background: #ecf5ff;
      }
    }
  }
}

.app-container .dynamic {
  .ownedContent {
    margin-top: 8px;
    min-height: 310px;
    height: calc(100vh - 320px);
    overflow-y: auto;
    padding-right: 2px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 999px;
      background: #d9dee7;
    }
  }

  .owned-privacy,
  .owned-empty {
    min-height: 180px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #eef1f6;
    border-radius: 10px;
    color: #909399;
    background: #fafbfc;
  }

  .owned-summary {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 14px;
  }

  .owned-stat-card {
    position: relative;
    overflow: hidden;
    padding: 16px 18px;
    border-radius: 12px;
    border: 1px solid transparent;

    &::after {
      content: '';
      position: absolute;
      right: -18px;
      top: -28px;
      width: 92px;
      height: 92px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.42);
    }

    &.games {
      background: linear-gradient(135deg, #ecf5ff 0%, #f7fbff 100%);
      border-color: #d8ebff;
    }

    &.value {
      background: linear-gradient(135deg, #fff3e6 0%, #fffaf3 100%);
      border-color: #ffe2bf;
    }

    strong {
      display: block;
      color: #14191e;
      font-size: 26px;
      line-height: 1.1;
      letter-spacing: 0;
    }

    span {
      display: block;
      margin-top: 6px;
      color: #68717d;
      font-size: 13px;
    }
  }

  .owned-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
    gap: 12px;
  }

  .owned-game-card {
    display: flex;
    gap: 10px;
    min-width: 0;
    padding: 10px;
    border: 1px solid #eef1f6;
    border-radius: 10px;
    background: #fff;
    cursor: pointer;
    transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

    &:hover {
      transform: translateY(-1px);
      border-color: #dce8f8;
      box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
    }

    img {
      width: 92px;
      height: 58px;
      object-fit: cover;
      border-radius: 8px;
      background: #f2f4f7;
      flex-shrink: 0;
    }
  }

  .owned-game-info {
    min-width: 0;
    flex: 1;

    h3 {
      margin: 0 0 5px;
      color: #14191e;
      font-size: 14px;
      line-height: 1.3;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    span {
      display: block;
      color: #7a828c;
      font-size: 12px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .owned-price-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 5px;
    margin-top: 6px;

    strong {
      color: #f56c6c;
      font-size: 14px;
    }

    em,
    i {
      font-style: normal;
      font-size: 11px;
      border-radius: 4px;
      padding: 1px 5px;
    }

    em {
      color: #f56c6c;
      background: #fff5f5;
    }

    i {
      color: #409eff;
      background: #ecf5ff;
    }
  }
}

.profile-view-content {
  padding: 12px 8px 4px;

  .privacy-notice {
    min-height: 130px;
    border-radius: 8px;
    background: #f7f9fc;
    color: #909399;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
  }

  .profile-view-row {
    display: grid;
    grid-template-columns: 86px minmax(0, 1fr);
    align-items: flex-start;
    gap: 16px;
    padding: 13px 6px;
    border-bottom: 1px solid #f0f2f5;

    span {
      color: #8a9199;
      font-size: 14px;
    }

    strong {
      min-width: 0;
      color: #303133;
      font-size: 14px;
      font-weight: 500;
      line-height: 1.6;
      word-break: break-word;
    }

    &:last-child {
      border-bottom: 0;
    }
  }

  .profile-signature-row strong {
    white-space: pre-wrap;
  }
}

.daily-task-summary {
  padding: 16px;
  border-radius: 8px;
  background: #f7f9fc;
  margin-bottom: 14px;

  strong {
    font-size: 26px;
    color: #409eff;
    margin-right: 6px;
  }

  span {
    color: #606266;
  }
}

.daily-task-list {
  display: grid;
  gap: 12px;
}

.daily-task-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.task-main {
  min-width: 0;
}

.task-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.task-desc {
  color: #909399;
  font-size: 13px;
}

.task-progress {
  width: 190px;
  flex-shrink: 0;
  color: #606266;
  font-size: 13px;
}

.statistics {
  .statisticsItem {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background-color: #f5f5f5;
    }
  }
}

.favorites-content {
  max-height: 600px;
  overflow-y: auto;

  .favorites-list {
    .favorite-item {
      padding: 15px;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        background-color: #f5f5f5;
      }

      .favorite-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .user-info {
          display: flex;
          align-items: center;
          gap: 8px;

          .username {
            font-size: 14px;
            color: #333;
          }

          .level {
            background-color: #329afe;
            color: #fff;
            font-size: 12px;
            padding: 1px 4px;
            border-radius: 3px;
          }
        }

        .game-tag {
          padding: 2px 8px;
          background-color: #e6f3ff;
          color: #329afe;
          border-radius: 4px;
          font-size: 12px;
        }
      }

      .favorite-title {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 8px;
        color: #333;
      }

      .favorite-content {
        font-size: 14px;
        color: #666;
        margin-bottom: 10px;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
      }

      .favorite-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .time {
          font-size: 12px;
          color: #999;
        }
      }

      .news-favorite-main {
        display: flex;
        gap: 14px;
      }

      .news-favorite-body {
        flex: 1;
        min-width: 0;
      }

      .news-favorite-cover {
        width: 132px;
        height: 82px;
        border-radius: 6px;
        overflow: hidden;
        flex-shrink: 0;
      }

      .news-actions {
        color: #909399;
        font-size: 12px;
      }
    }
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #ddd;
    border-radius: 3px;
  }
}

.user-list-content {
  max-height: 500px;
  overflow-y: auto;

  .user-list {
    .user-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .user-info {
        display: flex;
        align-items: center;
        gap: 12px;
        cursor: pointer;

        .user-details {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .username {
            font-size: 14px;
            font-weight: 500;
            color: #333;
          }

          .level {
            font-size: 12px;
            color: #fff;
            background-color: #329afe;
            padding: 0 4px;
            border-radius: 2px;
            display: inline-block;
          }
        }
      }
    }
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #ddd;
    border-radius: 3px;
  }
}

.list-search {
  margin-bottom: 12px;
}
</style>
