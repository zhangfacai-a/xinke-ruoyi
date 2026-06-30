<template>
  <div class="login">
    <div class="login-visual">
      <div class="brand-pill">Crucible ERP BI</div>
      <h1>电商数据操作系统</h1>
      <p>统一管理 ERP、BI、财务核算与经营决策，让运营和财务基于同一套数据工作。</p>
      <div class="visual-metrics">
        <span><strong>80.0%</strong>数据链路</span>
        <span><strong>24h</strong>财务看板</span>
        <span><strong>ROI</strong>经营分析</span>
      </div>
    </div>
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <p class="subtitle">登录到企业级经营分析后台</p>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" size="large" auto-complete="off" placeholder="密码"
          @keyup.enter="handleLogin">
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button :loading="loading" size="large" type="primary" style="width:100%;" @click.prevent="handleLogin">
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }]
}

const loading = ref(false)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
      })
    }
  })
}


function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCookie()
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 60px;
  height: 100%;
  padding: 64px 9vw;
  background:
    radial-gradient(circle at 12% 18%, rgba(162, 155, 254, 0.36), transparent 30%),
    radial-gradient(circle at 84% 16%, rgba(108, 92, 231, 0.18), transparent 28%),
    linear-gradient(135deg, #f7f8ff 0%, #eef1fb 100%);
  overflow: hidden;
}

.login::before {
  content: "";
  position: fixed;
  inset: 7% 6% auto auto;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.20), rgba(162, 155, 254, 0.14));
  filter: blur(8px);
}

.login-visual {
  position: relative;
  z-index: 1;
  max-width: 560px;
}

.brand-pill {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  color: #6c5ce7;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(108, 92, 231, 0.14);
  font-size: 13px;
  font-weight: 800;
}

.login-visual h1 {
  margin: 24px 0 14px;
  color: #202437;
  font-size: 44px;
  line-height: 1.18;
  font-weight: 850;
}

.login-visual p {
  max-width: 520px;
  margin: 0;
  color: #747d94;
  font-size: 16px;
  line-height: 1.9;
}

.visual-metrics {
  display: flex;
  gap: 14px;
  margin-top: 34px;
}

.visual-metrics span {
  min-width: 126px;
  padding: 16px;
  border-radius: 16px;
  color: #7a8297;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 18px 45px rgba(55, 62, 97, 0.08), inset 0 0 0 1px rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  font-size: 12px;
}

.visual-metrics strong {
  display: block;
  margin-bottom: 5px;
  color: #202437;
  font-size: 22px;
}

.title {
  margin: 0 auto 8px auto;
  text-align: center;
  color: #202437;
  font-size: 24px;
  font-weight: 850;
}

.subtitle {
  margin: 0 0 26px;
  text-align: center;
  color: #8a91a6;
  font-size: 13px;
}

.login-form {
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  width: 420px;
  padding: 34px 34px 18px;
  z-index: 1;
  box-shadow: 0 24px 70px rgba(55, 62, 97, 0.16);
  backdrop-filter: blur(20px);

  .el-input {
    height: 46px;

    input {
      height: 46px;
    }
  }

  .input-icon {
    height: 45px;
    width: 14px;
    margin-left: 0px;
    color: #6c5ce7;
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-code {
  width: 33%;
  height: 46px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-code-img {
  height: 46px;
  padding-left: 12px;
  border-radius: 12px;
}

@media (max-width: 960px) {
  .login {
    justify-content: center;
    padding: 24px;
  }

  .login-visual {
    display: none;
  }

  .login-form {
    width: min(420px, 100%);
  }
}
</style>
