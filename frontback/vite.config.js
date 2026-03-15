import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 🌟 修改这一行：尝试用这种解构或者默认引用的方式
import monacoEditorPluginModule from 'vite-plugin-monaco-editor'

// 兼容性处理
const isObject = (val) => val && typeof val === 'object' && !Array.isArray(val);
const monacoEditorPlugin = isObject(monacoEditorPluginModule) && monacoEditorPluginModule.default
    ? monacoEditorPluginModule.default
    : monacoEditorPluginModule;

export default defineConfig({
    plugins: [
        vue(),
        // 这里的调用现在应该正常了
        monacoEditorPlugin({
            languageWorkers: ['editorWorkerService', 'typescript', 'json']
        })
    ],
    build: {
        rollupOptions: {
            input: {
                main: resolve(__dirname, 'index.html'),
                // 如果你之前的 list.html 和 detail.html 还在，保留它们
                list: resolve(__dirname, 'public/list.html'),
                detail: resolve(__dirname, 'public/detail.html')
            }
        }
    }
})