import '@fontsource-variable/inter/wght.css'
import '@fontsource-variable/jetbrains-mono/wght.css'
import './assets/tokens.css'
import './assets/base.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { configureApi } from './lib/api'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

const auth = useAuthStore(pinia)

configureApi({
  getAccessToken: () => auth.accessToken,
  refresh: () => auth.refresh(),
  onAccountSuspended: () => auth.markSuspended(),
})

app.use(router)
app.use(i18n)

app.mount('#app')
