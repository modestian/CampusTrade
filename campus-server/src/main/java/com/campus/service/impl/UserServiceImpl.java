package com.campus.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.JwtClaimsConstant;
import com.campus.common.constant.MessageConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.context.UserContext;
import com.campus.common.exception.AccountLockedException;
import com.campus.common.exception.BusinessException;
import com.campus.common.properties.JwtProperties;
import com.campus.common.result.PageResult;
import com.campus.common.utils.ColorPalette;
import com.campus.common.utils.JwtUtil;
import com.campus.common.utils.PasswordEncoder;
import com.campus.common.utils.RegexUtils;
import com.campus.mapper.UserMapper;
import com.campus.pojo.dto.LoginDTO;
import com.campus.pojo.dto.PasswordEditDTO;
import com.campus.pojo.dto.RegisterDTO;
import com.campus.pojo.dto.UserUpdateDTO;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.UserLoginVO;
import com.campus.pojo.vo.UserVO;
import com.campus.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学生用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtProperties jwtProperties;

    @Override
    @Transactional
    public UserLoginVO register(RegisterDTO registerDTO) {
        if (!RegexUtils.isUsername(registerDTO.getUsername())) {
            throw new BusinessException(MessageConstant.USERNAME_FORMAT_ERROR);
        }
        if (!RegexUtils.isPassword(registerDTO.getPassword())) {
            throw new BusinessException(MessageConstant.PASSWORD_FORMAT_ERROR);
        }
        if (StrUtil.isBlank(registerDTO.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }
        Integer exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (exists > 0) {
            throw new BusinessException(MessageConstant.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setNickname(registerDTO.getNickname());
        user.setPassword(PasswordEncoder.encode(registerDTO.getPassword()));
        user.setAvatarColor(ColorPalette.normalize(registerDTO.getAvatarColor()));
        user.setCredit(100);
        user.setStatus(0);
        userMapper.insert(user);

        return buildLoginResult(user);
    }

    @Override
    public UserLoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!PasswordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(MessageConstant.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new AccountLockedException();
        }
        return buildLoginResult(user);
    }

    @Override
    public void logout(String token) {
        stringRedisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + token);
    }

    @Override
    public UserVO getMe() {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public void updateProfile(UserUpdateDTO userUpdateDTO) {
        if (StrUtil.isNotBlank(userUpdateDTO.getPhone()) && !RegexUtils.isPhone(userUpdateDTO.getPhone())) {
            throw new BusinessException(MessageConstant.PHONE_FORMAT_ERROR);
        }
        User update = new User();
        update.setId(UserContext.getUserId());
        update.setNickname(StrUtil.isBlank(userUpdateDTO.getNickname()) ? null : userUpdateDTO.getNickname());
        update.setAvatarColor(StrUtil.isBlank(userUpdateDTO.getAvatarColor()) ? null
                : ColorPalette.normalize(userUpdateDTO.getAvatarColor()));
        update.setPhone(userUpdateDTO.getPhone());
        update.setCampus(userUpdateDTO.getCampus());
        userMapper.updateById(update);
    }

    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        if (!RegexUtils.isPassword(passwordEditDTO.getNewPassword())) {
            throw new BusinessException(MessageConstant.PASSWORD_FORMAT_ERROR);
        }
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null || !PasswordEncoder.matches(passwordEditDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(MessageConstant.OLD_PASSWORD_ERROR);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(PasswordEncoder.encode(passwordEditDTO.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public PageResult<UserVO> pageUsers(String keyword, Integer status, int page, int size) {
        Page<User> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword));
        }
        if (status != null) {
            qw.eq(User::getStatus, status);
        }
        qw.orderByDesc(User::getCreateTime);
        userMapper.selectPage(mpPage, qw);
        List<UserVO> records = mpPage.getRecords().stream().map(this::toUserVO).collect(Collectors.toList());
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), records);
    }

    @Override
    public UserVO getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    /* ---------- 私有方法 ---------- */

    private UserLoginVO buildLoginResult(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJwt(jwtProperties.getStudentSecretKey(), jwtProperties.getStudentTtl(), claims);

        // Redis 登录态（精简 JSON：id / username / nickname / avatarColor / status）
        JSONObject loginUser = new JSONObject();
        loginUser.put("id", user.getId());
        loginUser.put("username", user.getUsername());
        loginUser.put("nickname", user.getNickname());
        loginUser.put("avatarColor", user.getAvatarColor());
        loginUser.put("status", user.getStatus());
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_TOKEN_KEY + token,
                loginUser.toJSONString(), RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUser(toUserVO(user));
        return vo;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
