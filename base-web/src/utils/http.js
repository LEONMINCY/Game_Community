// 文件说明：utils/http.js，业务请求工具，封装GET、POST、PUT和DELETE请求的统一错误处理。
import axios from './api.js';
/**
 * 发送GET请求，用于列表、详情和统计数据查询。
 */
export function getRequest(url, config = {}) {
	return new Promise(async (resolve, reject) => {
		try {
			let res = null;
			res = await axios.get(url, config);

			resolve(res);
		} catch (error) {
			let errorMsg = `请求报错路径： ${url} \n 请求错误信息: ${error}`;
			console.log(errorMsg);
			reject(error);
		}
	});
}

/**
 * 发送POST请求，用于新增、登录、提交表单和状态切换。
 */
export function postRequest(url, data, config = {}) {
	return new Promise(async (resolve, reject) => {
		try {
			let res = await axios.post(url, data, config);
			resolve(res);
		} catch (error) {
			let errorMsg = `请求报错路径：${url} \n 请求错误信息: ${error}`;
			reject(error);
		}
	});
}

/**
 * 发送PUT请求，用于保存编辑后的业务数据。
 */
export function putRequest(url, data) {
	return new Promise(async (resolve, reject) => {
		try {
			let res = await axios.put(url, data);
			resolve(res);
		} catch (error) {
			let errorMsg = `请求报错路径：${url} \n 请求错误信息: ${error}`;
			reject(error);
		}
	});
}

/**
 * 发送DELETE请求，用于删除记录、取消关注和移除购物车等操作。
 */
export function deleteRequest(url, data) {
	return new Promise(async (resolve, reject) => {
		try {
			let res = await axios.delete(url, data);
			resolve(res);
		} catch (error) {
			let errorMsg = `请求报错路径：${url} \n 请求错误信息: ${error}`;
			reject(error);
		}
	});
}
