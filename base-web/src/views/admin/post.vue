<!-- 文件说明：views/admin/post.vue，后台帖子管理页面，审核帖子、查看详情、处理申诉和评论。 -->
<template>
  <div class="post-management">
    <el-form :model="queryParams" label-width="40px" :inline="true">
      <el-form-item label="标题">
        <el-input v-model="queryParams.title" placeholder="请输入标题"></el-input>
      </el-form-item>
      <el-form-item label="审核">
        <el-select v-model="queryParams.auditStatus" clearable placeholder="审核状态" style="width: 130px;">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item label="申诉">
        <el-select v-model="queryParams.appealStatus" clearable placeholder="申诉状态" style="width: 130px;">
          <el-option label="未申诉" value="none" />
          <el-option label="申诉中" value="pending" />
          <el-option label="申诉通过" value="approved" />
          <el-option label="申诉拒绝" value="rejected" />
        </el-select>
      </el-form-item>
      <!-- <el-form-item label="游戏标签">
        <el-select v-model="queryParams.tag" placeholder="请选择游戏标签" style="width: 200px;">
          <el-option v-for="tag in gameTags" :key="tag" :label="tag" :value="tag"></el-option>
        </el-select>
      </el-form-item> -->
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button icon="Refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          size="mini"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
    </el-row>
    <el-table :data="posts" class="posts-table" style="width: 100%" table-layout="fixed">
      <el-table-column type="expand" width="44">
        <template #default="scope">
          <div class="post-expand">
            <p><strong>审核原因：</strong>{{ scope.row.auditReason || '无' }}</p>
            <p><strong>申诉内容：</strong>{{ scope.row.appealContent || '无' }}</p>
            <p><strong>申诉回复：</strong>{{ scope.row.appealReply || '无' }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip></el-table-column>
      <el-table-column prop="createTime" label="时间" width="178" show-overflow-tooltip></el-table-column>
      <el-table-column prop="gameName" label="游戏标签" min-width="170" show-overflow-tooltip>
        <template v-slot="scope">
            <gameTag :src="scope.row.gameIcon" :name="scope.row.gameName"></gameTag>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="用户" width="96" show-overflow-tooltip></el-table-column>
      <el-table-column label="审核状态" width="96">
        <template v-slot="scope">
          <el-tag :type="auditTagType(scope.row.auditStatus)">{{ auditText(scope.row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申诉状态" width="96">
        <template v-slot="scope">
          <el-tag :type="appealTagType(scope.row.appealStatus)">{{ appealText(scope.row.appealStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template v-slot="scope">
          <div class="table-actions">
            <el-button type="primary" link @click="openDetailDialog(scope.row)">查看</el-button>
            <el-button v-if="scope.row.auditStatus === 'pending'" type="success" link @click="auditPost(scope.row, 'approved')">通过</el-button>
            <el-button v-if="scope.row.auditStatus === 'pending'" type="danger" link @click="openRejectDialog(scope.row)">拒绝</el-button>
            <el-button v-if="scope.row.appealStatus === 'pending'" type="primary" link @click="openAppealDialog(scope.row)">处理申诉</el-button>
            <el-button type="warning" link @click="editPost(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="deletePost(scope.row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: end;">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="totalPosts"
        layout="total, prev, pager, next, jumper">
      </el-pagination>
    </div>

    <el-dialog v-model="detailDialogVisible" title="帖子详情" width="780px">
      <div v-if="detailPost" class="admin-post-detail">
        <div class="detail-head">
          <h2>{{ detailPost.title }}</h2>
          <div class="detail-meta">
            <span>作者：{{ detailPost.nickname || '-' }}</span>
            <span>游戏：{{ detailPost.gameName || '-' }}</span>
            <span>发布时间：{{ detailPost.createTime || '-' }}</span>
          </div>
        </div>

        <div class="detail-content" v-html="detailPost.content"></div>

        <div v-if="detailImages.length" class="detail-images">
          <el-image
            v-for="img in detailImages"
            :key="img"
            :src="img"
            fit="cover"
            preview-teleported
            :preview-src-list="detailImages"
          />
        </div>

        <div class="comment-section">
          <div class="section-title">评论区</div>
          <el-empty v-if="detailComments.length === 0" description="暂无评论" />
          <div v-else class="comment-list">
            <div v-for="comment in detailComments" :key="comment.id" class="comment-card">
              <div class="comment-main">
                <el-avatar :size="32" :src="replaceURL(comment.avatar || '')" />
                <div class="comment-body">
                  <div class="comment-meta">
                    <span>{{ comment.nickname || ('用户' + comment.userId) }}</span>
                    <small>{{ comment.createTime || '-' }}</small>
                  </div>
                  <div class="comment-text">{{ comment.content || '-' }}</div>
                  <el-image
                    v-if="comment.imageUrl"
                    class="comment-image"
                    :src="replaceURL(comment.imageUrl)"
                    fit="cover"
                    preview-teleported
                    :preview-src-list="[replaceURL(comment.imageUrl)]"
                  />
                  <div v-if="comment.children && comment.children.length" class="reply-list">
                    <div v-for="reply in comment.children" :key="reply.id" class="reply-card">
                      <strong>{{ reply.nickname || ('用户' + reply.userId) }}：</strong>{{ reply.content || '-' }}
                      <el-image
                        v-if="reply.imageUrl"
                        class="reply-image"
                        :src="replaceURL(reply.imageUrl)"
                        fit="cover"
                        preview-teleported
                        :preview-src-list="[replaceURL(reply.imageUrl)]"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="admin-comment-composer" @click.stop>
            <div class="composer-toolbar">
              <el-button text circle title="表情" @click="showAdminCommentEmoji = !showAdminCommentEmoji">
                <el-icon><Sunny /></el-icon>
              </el-button>
              <el-upload
                action="/api/file/upload"
                :headers="headers"
                :show-file-list="false"
                accept="image/*"
                :on-success="handleAdminCommentImageSuccess"
              >
                <el-button text circle title="发送图片">
                  <el-icon><Picture /></el-icon>
                </el-button>
              </el-upload>
            </div>
            <div v-if="showAdminCommentEmoji" class="admin-emoji-panel">
              <span v-for="emoji in adminEmojiList" :key="emoji" @click="insertAdminCommentEmoji(emoji)">{{ emoji }}</span>
            </div>
            <el-input
              v-model="adminCommentContent"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="以审核员身份发表评论..."
            />
            <div v-if="adminCommentImageUrl" class="admin-comment-image-preview">
              <el-image :src="replaceURL(adminCommentImageUrl)" fit="cover" />
              <el-button text type="danger" @click="adminCommentImageUrl = ''">移除图片</el-button>
            </div>
            <div class="composer-actions">
              <el-button
                type="primary"
                :disabled="!adminCommentContent.trim() && !adminCommentImageUrl"
                @click="submitAdminComment"
              >发表评论</el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑帖子' : '新增帖子'" width="50%">
      <div style="padding: 30px;">
        <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入标题"></el-input>
          </el-form-item>

          <el-form-item label="内容" prop="content">
            <div class="editor-container">
              <Toolbar
                style="border-bottom: 1px solid #ccc"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                mode="default"
              />
              <Editor
                style="height: 400px"
                v-model="form.content"
                :defaultConfig="editorConfig"
                mode="default"
                @onCreated="handleCreated"
              />
            </div>
          </el-form-item>

          <el-form-item label="游戏标签" prop="gameId">
            <el-select v-model="form.gameId" placeholder="请选择游戏">
              <el-option
                v-for="game in gameList"
                :key="game.id"
                :label="game.name"
                :value="game.id"
              >
                <gameTag :src="game.icon" :name="game.name" />
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="封面">
            <el-upload
              v-model:file-list="fileList"
              action="/api/file/upload"
              :headers="headers"
              list-type="picture-card"
              :limit="5"
              :on-change="handleImageChange"
              :on-success="handleImageSuccess"
              :on-remove="handleImageRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="addPost">确定</el-button>
            <el-button @click="dialogVisible = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝帖子" width="420px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入拒绝原因"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealDialogVisible" title="处理申诉" width="500px">
      <div class="appeal-detail">
        <p><strong>申诉内容：</strong>{{ currentPost?.appealContent || '无' }}</p>
        <p><strong>原拒绝原因：</strong>{{ currentPost?.auditReason || '无' }}</p>
      </div>
      <el-input
        v-model="appealReply"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入处理回复"
      />
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitAppealReview('rejected')">驳回申诉</el-button>
        <el-button type="success" @click="submitAppealReview('approved')">通过申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef, onBeforeUnmount } from 'vue';
import { postRequest, getRequest, putRequest, deleteRequest } from '../../utils/http';
import gameTag from '../../components/gameTag.vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Picture, Plus, Sunny } from '@element-plus/icons-vue';
import { emojiList } from '@/utils/emoji';
import { replaceURL } from '@/utils/tools';
import '@wangeditor/editor/dist/css/style.css';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';

const currentPage = ref(1);
const pageSize = ref(10);
const totalPosts = ref(0);
const gameTags = ref(['动作', '冒险', '角色扮演']); // 游戏标签示例
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  title: null,
  tag: null,
  auditStatus: null,
  appealStatus: null,
});
const form = ref({
  title: null,
  content: null,
  gameId: null,
  images: []
});
const dialogVisible = ref(false);
const posts = ref([]); // 所有帖子数据
const formRef = ref(null);
const fileList = ref([]);
const headers = ref({
  Authorization: localStorage.getItem('token')
});
const rejectDialogVisible = ref(false);
const appealDialogVisible = ref(false);
const rejectReason = ref('');
const appealReply = ref('');
const currentPost = ref(null);
const detailDialogVisible = ref(false);
const detailPost = ref(null);
const detailComments = ref([]);
const adminCommentContent = ref('');
const adminCommentImageUrl = ref('');
const showAdminCommentEmoji = ref(false);
const adminEmojiList = emojiList;

// 游戏列表数据
const gameList = ref([]);

const detailImages = computed(() => {
  if (!detailPost.value?.media) {
    return []
  }
  try {
    const images = JSON.parse(detailPost.value.media)
    return Array.isArray(images) ? images.map(img => replaceURL(img)) : []
  } catch (error) {
    return []
  }
});

// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef();

// 工具栏配置
const toolbarConfig = {
  excludeKeys: []
};

// 编辑器配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/file/upload',
      headers: {
        Authorization: localStorage.getItem('token')
      },
      fieldName: 'file',
      onBeforeUpload(file) {
        return file
      },
      customInsert(res, insertFn) {
        if (res.code === 200) {
          const url = res.data
          insertFn(url)
        } else {
          ElMessage.error('图片上传失败')
        }
      }
    }
  }
};

onMounted(() => {
  getPostList();
  getGameList();
});

const paginatedPosts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return posts.value.slice(start, start + pageSize.value);
});

// 分页获取帖子列表
const getPostList = () => {
  postRequest('/post/page', queryParams.value).then(res => {
    console.log(res);
    if (res.code === 200) {
      posts.value = res.data.list

      totalPosts.value = res.data.total
    }
  });
};

//获取游戏列表数据
const getGameList = () => {
  getRequest('/game/listAll').then(res => {
    console.log(res)
    if (res.code === 200) {
        gameList.value = res.data
    }
  })
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    title: null,
    tag: null,
    auditStatus: null,
    appealStatus: null,
  }
  getPostList()
}
 const search = () => {
  getPostList()
}
const handleCurrentChange = (page) => {
  currentPage.value = page;
  queryParams.value.pageNo = page; // 同步更新 queryParams.pageNo
  getPostList(); // 重新获取帖子列表
};

const handleAdd = () => {
  dialogVisible.value = true;
  resetForm();
};

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 50, message: '标题长度在2到50个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 10, max: 10000, message: '内容长度在10到10000个字符之间', trigger: 'blur' }
  ],
  gameId: [
    { required: true, message: '请选择游戏标签', trigger: 'change' }
  ]
};

// 处理图片上传成功
const handleImageSuccess = (response, uploadFile) => {
  console.log(response)
  if (response.code === 200) {
    form.value.images.push(response.data)
    // 更新上传文件的 url
    uploadFile.url = response.data
  }
}

// 处理图片变化
const handleImageChange = (uploadFile) => {
  if (fileList.value.length > 5) {
    ElMessage.warning('最多只能上传5张图片')
    return
  }
}

const handleImageRemove = (file) => {
  // 从 form.images 中移除对应的图片URL
  const index = form.value.images.indexOf(file.url)
  if (index > -1) {
    form.value.images.splice(index, 1)
  }
}

const addPost = () => {
  if (!formRef.value) return

  formRef.value.validate((valid) => {
    if (valid) {
      const postData = {
        id: form.value.id,
        title: form.value.title,
        content: form.value.content,
        gameId: form.value.gameId,
        media: JSON.stringify(form.value.images)
      };

      const request = form.value.id ?
        putRequest('/post/update', postData) :
        postRequest('/post/admin/add', postData);

      request.then(res => {
        if (res.code === 200) {
          ElMessage.success(form.value.id ? '更新成功' : '发布成功')
          dialogVisible.value = false
          getPostList()
          resetForm()
        }
      }).catch(err => {
        console.log(err)
        resetForm()
      })
    }
  })
};

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
});

const handleCreated = (editor) => {
  editorRef.value = editor
};

const editPost = (post) => {
  resetForm();
  form.value = {
    id: post.id,
    title: post.title,
    content: post.content,
    gameId: post.gameId,
    images: post.media ? JSON.parse(post.media) : []
  };
  if (post.media) {
    fileList.value = JSON.parse(post.media).map(url => ({
      url,
      name: url.substring(url.lastIndexOf('/') + 1)
    }));
  }
  dialogVisible.value = true;
};

const openDetailDialog = (post) => {
  detailPost.value = post
  detailComments.value = []
  adminCommentContent.value = ''
  adminCommentImageUrl.value = ''
  showAdminCommentEmoji.value = false
  detailDialogVisible.value = true
  loadPostComments(post.id)
};

const loadPostComments = (postId) => {
  postRequest('/comment/list', {
    postId,
    pageNo: 1,
    pageSize: 100
  }).then(res => {
    if (res.code === 200) {
      const rows = res.data.list || []
      detailComments.value = rows.map(item => ({
        ...item,
        children: []
      }))
      detailComments.value.forEach(comment => loadCommentReplies(comment))
    }
  }).catch(() => {
    ElMessage.error('评论加载失败')
  })
};

const loadCommentReplies = (comment) => {
  getRequest(`/comment/reply/list/${comment.id}?pageNo=1&pageSize=50`).then(res => {
    if (res.code === 200) {
      comment.children = res.data.list || []
    }
  })
};

const insertAdminCommentEmoji = (emoji) => {
  adminCommentContent.value += emoji
  showAdminCommentEmoji.value = false
};

const handleAdminCommentImageSuccess = (res) => {
  if (res.code === 200 && res.data) {
    adminCommentImageUrl.value = res.data
    ElMessage.success('图片已添加')
  } else {
    ElMessage.error(res.msg || '图片上传失败')
  }
};

const submitAdminComment = () => {
  if (!detailPost.value?.id) return
  if (!adminCommentContent.value.trim() && !adminCommentImageUrl.value) {
    ElMessage.warning('评论内容或图片不能为空')
    return
  }
  postRequest('/comment/add', {
    postId: detailPost.value.id,
    content: adminCommentContent.value.trim(),
    imageUrl: adminCommentImageUrl.value
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('评论已发表')
      adminCommentContent.value = ''
      adminCommentImageUrl.value = ''
      showAdminCommentEmoji.value = false
      loadPostComments(detailPost.value.id)
    } else {
      ElMessage.error(res.msg || '评论发表失败')
    }
  })
};

const deletePost = (postId) => {
  ElMessageBox.confirm('确认要删除这条帖子吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    deleteRequest(`/post/delete/${postId}`).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功');
        getPostList();
      } else {
        ElMessage.error(res.msg || '删除失败');
      }
    }).catch(err => {
      console.error(err);
      ElMessage.error('删除失败');
    });
  }).catch(() => {
    // 用户点击取消，不做任何操作
  });
};

const auditText = (status) => {
  return {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }[status || 'approved'] || '已通过'
};

const auditTagType = (status) => {
  return {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }[status || 'approved'] || 'success'
};

const appealText = (status) => {
  return {
    none: '未申诉',
    pending: '申诉中',
    approved: '申诉通过',
    rejected: '申诉拒绝'
  }[status || 'none'] || '未申诉'
};

const appealTagType = (status) => {
  return {
    none: 'info',
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }[status || 'none'] || 'info'
};

const auditPost = (post, status, reason = '') => {
  putRequest('/post/audit/' + post.id, {
    auditStatus: status,
    auditReason: reason
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success(status === 'approved' ? '审核通过' : '已拒绝')
      getPostList()
    }
  })
};

const openRejectDialog = (post) => {
  currentPost.value = post
  rejectReason.value = post.auditReason || ''
  rejectDialogVisible.value = true
};

const submitReject = () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  auditPost(currentPost.value, 'rejected', rejectReason.value)
  rejectDialogVisible.value = false
};

const openAppealDialog = (post) => {
  currentPost.value = post
  appealReply.value = post.appealReply || ''
  appealDialogVisible.value = true
};

const submitAppealReview = (status) => {
  putRequest('/post/appeal/audit/' + currentPost.value.id, {
    appealStatus: status,
    appealReply: appealReply.value
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success(status === 'approved' ? '申诉已通过' : '申诉已驳回')
      appealDialogVisible.value = false
      getPostList()
    }
  })
};

const resetForm = () => {
  if (!formRef.value) return
  formRef.value.resetFields()
  fileList.value = []
  form.value = {
    title: null,
    content: '',
    gameId: null,
    images: []
  }
  // 重置编辑器内容
  if (editorRef.value) {
    editorRef.value.clear()
  }
};
</script>

<style scoped>
.post-management {
  padding: 40px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow-x: hidden;
}

.posts-table {
  max-width: 100%;
}

.posts-table :deep(.el-table__inner-wrapper),
.posts-table :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.posts-table :deep(.el-table__cell) {
  box-sizing: border-box;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0;
}

.post-expand,
.appeal-detail {
  padding: 8px 24px;
  color: #606266;
  line-height: 1.7;
}

.editor-container {
  border: 1px solid #ccc;
  z-index: 100;
  width: 100%;
  margin-bottom: 20px;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
}

:deep(.w-e-text-container) {
  background-color: #fff;
}

:deep(.w-e-toolbar) {
  border-bottom: 1px solid #eee;
  background-color: #fafafa;
}

:deep(.w-e-text-placeholder) {
  color: #999;
}

:deep(.w-e-text-container [data-slate-editor]) {
  padding: 0 10px;
}

.admin-post-detail {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

.detail-head h2 {
  margin: 0 0 10px;
  color: #1f2d3d;
  font-size: 22px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}

.detail-content {
  margin-top: 16px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 8px;
  line-height: 1.8;
  color: #303133;
}

.detail-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.detail-images :deep(.el-image) {
  height: 90px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.comment-section {
  margin-top: 18px;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.section-title {
  margin-bottom: 12px;
  font-weight: 700;
  color: #1f2d3d;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.comment-main {
  display: flex;
  gap: 10px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #909399;
  font-size: 12px;
}

.comment-text {
  margin-top: 6px;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}

.comment-image,
.reply-image {
  width: 96px;
  height: 72px;
  margin-top: 8px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.reply-list {
  margin-top: 8px;
  padding: 8px;
  background: #f7f9fc;
  border-radius: 6px;
}

.reply-card {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
</style>
