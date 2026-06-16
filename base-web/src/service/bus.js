// 文件说明：全局事件总线，用于跨组件传递轻量级交互事件。
import mitt from 'mitt'

const bus = mitt()

export default bus
