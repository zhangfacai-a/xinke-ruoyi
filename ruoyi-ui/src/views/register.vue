<template>
  <div class="register">
    <div class="register-visual">
      <div class="brand-pill">Crucible ERP BI</div>
      <h1>建立统一的数据工作台</h1>
      <p>为运营、财务、仓储和管理者提供一致的业务口径与清晰的分析界面。</p>
    </div>
    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <h3 class="title">{{ title }}</h3>
      <p class="subtitle">创建账户后进入企业级数据后台</p>
      <el-form-item prop="username">
        <el-input 
          v-model="registerForm.username" 
          type="text" 
          size="large" 
          auto-complete="off" 
          placeholder="账号"
        >
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password" :rules="registerPwdValidator">
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large" 
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large" 
          auto-complete="off"
          placeholder="确认密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          size="large" 
          v-model="registerForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="register-code">
          <img :src="codeUrl" @click="getCode" class="register-code-img"/>
        </div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large" 
          type="primary"
          style="width:100%;"
          @click.prevent="handleRegister"
        >
          <span v-if="!loading">注 册</span>
          <span v-else>注 册 中...</span>
        </el-button>
        <div style="float: right;">
          <router-link class="link-type" :to="'/login'">使用已有账户登录</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'
import { usePasswordRule } from "@/utils/passwordRule"

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()
const { registerPwdValidator } = usePasswordRule()

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style lang='scss' scoped>
.register {
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
.register-visual {
  position: relative;
  z-index: 1;
  max-width: 540px;
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
.register-visual h1 {
  margin: 24px 0 14px;
  color: #202437;
  font-size: 44px;
  line-height: 1.18;
  font-weight: 850;
}
.register-visual p {
  max-width: 500px;
  margin: 0;
  color: #747d94;
  font-size: 16px;
  line-height: 1.9;
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

.register-form {
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
.register-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.register-code {
  width: 33%;
  height: 46px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-register-footer {
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
.register-code-img {
  height: 46px;
  padding-left: 12px;
  border-radius: 12px;
}

@media (max-width: 960px) {
  .register {
    justify-content: center;
    padding: 24px;
  }
  .register-visual {
    display: none;
  }
  .register-form {
    width: min(420px, 100%);
  }
}
</style>
