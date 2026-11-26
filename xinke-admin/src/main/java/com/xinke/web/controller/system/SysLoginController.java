package com.xinke.web.controller.system;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.constant.Constants;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.domain.entity.SysMenu;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.core.domain.model.LoginBody;
import com.xinke.common.core.domain.model.LoginUser;
import com.xinke.common.core.text.Convert;
import com.xinke.common.utils.DateUtils;
import com.xinke.common.utils.SecurityUtils;
import com.xinke.common.utils.StringUtils;
import com.xinke.framework.web.service.SysLoginService;
import com.xinke.framework.web.service.SysPermissionService;
import com.xinke.framework.web.service.TokenService;
import com.xinke.system.service.ISysConfigService;
import com.xinke.system.service.ISysMenuService;
import com.xinke.erp.service.FreightAccessService;

/**
 * 登录验证
 * 
 * @author xinke
 */
@RestController
public class SysLoginController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private FreightAccessService freightAccess;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        var freightPolicy=freightAccess.current();
        Set<String> displayedPermissions=new java.util.HashSet<>(permissions);
        if(freightPolicy!=null) {
            displayedPermissions.removeIf(p->p.startsWith("live:freight:"));
            if(freightPolicy.actions().contains("view")) displayedPermissions.add("live:freight:entry");
        }
        ajax.put("permissions", displayedPermissions);
        ajax.put("pwdChrtype", getSysAccountChrtype());
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        var policy=freightAccess.current();
        if(policy!=null && !policy.actions().contains("view")) removeFreightMenu(menus);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    private void removeFreightMenu(List<SysMenu> menus) {
        menus.removeIf(menu->"live/freight/index".equals(menu.getComponent()));
        for(var menu:menus) removeFreightMenu(menu.getChildren());
        menus.removeIf(menu->"ops-freight".equals(menu.getPath()) && menu.getChildren().isEmpty());
    }

    // 获取用户密码自定义配置规则
    public String getSysAccountChrtype()
    {
        return Convert.toStr(configService.selectConfigByKey("sys.account.chrtype"), "0");
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
