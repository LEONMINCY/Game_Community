// 文件说明：前端基础请求实例，统一处理登录校验、请求头和接口错误提示。
import axios from 'axios';
import router from '../router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { buildLoginLocation, isPublicPostUrl } from '@/utils/auth'

const BASE_URL = '/api';

// 请求基础配置。
axios.defaults.baseURL = BASE_URL;
axios.defaults.timeout = 5000;

// 请求拦截器
axios.interceptors.request.use(
	(config) => {
		config.withCredentials = true;
		let token = localStorage.getItem('token');

		if (token) {
			config.headers['Authorization'] = token;
		} else if (requiresLogin(config)) {
			ElMessage.warning('请先登录后再操作');
			router.push(buildLoginLocation(router.currentRoute.value.fullPath));
			return Promise.reject(new Error('NEED_LOGIN'));
		}
		if (config.method === 'get') {
			config.params = {
				t: Date.parse(new Date()) / 1000,
				...config.params,
			};
		}
		return config;
	},
	(error) => {
		return Promise.reject(error);
	}
);

// 响应拦截器。
axios.interceptors.response.use(
	(response) => {
		/**
		 * code为非200是抛错 可结合自己业务进行修改
		 */
		const res = response.data;
		if (res.code !== 200) {
			// 401:未登录;
			if (res.code === 401) {
				handleUnauthorized();
			} else {
				ElMessage.error(res.msg);
			}
			return Promise.reject('error');
		} else {
			return response.data;
		}
	},
	(error) => {
		console.log('err' + error); // 调试日志。
		if (error && error.message !== 'NEED_LOGIN') {
			ElMessage.error(error.message);
		}
		return Promise.reject(error);
	}
);

function requiresLogin(config) {
	const method = String(config.method || 'get').toLowerCase();
	const url = String(config.url || '');
	if (method === 'get' || method === 'head' || method === 'options') {
		return false;
	}
	if (method === 'post' && isPublicPostUrl(url)) {
		return false;
	}
	return true;
}

function handleUnauthorized() {
	const hasToken = Boolean(localStorage.getItem('token'));
	if (!hasToken) {
		ElMessage.warning('请先登录后再操作');
		router.push(buildLoginLocation(router.currentRoute.value.fullPath));
		return;
	}
	ElMessageBox.confirm('登录状态已失效，可以取消继续留在该页面，或者重新登录', '登录失效', {
		confirmButtonText: '重新登录',
		cancelButtonText: '取消',
		type: 'warning',
	}).then(() => {
		localStorage.clear();
		router.push(buildLoginLocation(router.currentRoute.value.fullPath));
	}).catch(() => {});
}

export default axios;
