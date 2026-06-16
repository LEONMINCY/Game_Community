import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
// import eslintPlugin from 'vite-plugin-eslint';
// import inject from '@rollup/plugin-inject';
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // eslintPlugin({
    //  include: ['src/**/*.ts', 'src/**/*.vue', 'src/*.ts', 'src/*.vue'],
    // }),
    // inject({
    //   'window.Quill': ['@vueup/vue-quill','Quill'],
    //   Quill: ['@vueup/vue-quill','Quill'],
    //   // 'window.Quill': path.resolve('./node_modules/quill/dist/quill.js'),
    // }),
  ],
  resolve: {
    // 配置路径别名
    alias: {
      '@': resolve(__dirname, './src'),
    },
    extensions: ['.js', '.json', '.ts'],
  },
  css: {
    // css预处理器
    preprocessorOptions: {
      scss: {
        // 引入 mixin.scss 这样就可以在全局中使用 mixin.scss中预定义的变量了
        // 给导入的路径最后加上 ;
        additionalData: '@import "@/assets/style/mixin.scss";',
      },
    },
  },
  server: {
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/noLogin': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:9999',
        changeOrigin: true
      }
    },
  },
  build: {
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus', '@element-plus/icons-vue'],
          editor: ['@wangeditor/editor', '@wangeditor/editor-for-vue']
        }
      }
    }
  },
});
