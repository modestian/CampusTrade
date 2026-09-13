package com.campus.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.constant.JwtClaimsConstant;
import com.campus.common.constant.MessageConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.context.UserContext;
import com.campus.common.exception.AccountLockedException;
import com.campus.common.exception.BusinessException;
import com.campus.common.properties.JwtProperties;
import com.campus.common.utils.JwtUtil;
import com.campus.common.utils.PasswordEncoder;
import com.campus.mapper.AdminMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReportMapper;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.dto.LoginDTO;
import com.campus.pojo.entity.Admin;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.ProductOrder;
import com.campus.pojo.entity.Report;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.AdminLoginVO;
import com.campus.pojo.vo.AdminVO;
import com.campus.pojo.vo.WorkspaceVO;
import com.campus.service.AdminService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员服务实现
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public AdminLoginVO login(LoginDTO loginDTO) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, loginDTO.getUsername()));
        if (admin == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!PasswordEncoder.matches(loginDTO.getPassword(), admin.getPassword())) {
            throw new BusinessException(MessageConstant.PASSWORD_ERROR);
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new AccountLockedException();
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.ADMIN_ID, admin.getId());
        String token = JwtUtil.createJwt(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);

        JSONObject loginAdmin = new JSONObject();
        loginAdmin.put("id", admin.getId());
        loginAdmin.put("username", admin.getUsername());
        loginAdmin.put("name", admin.getName());
        loginAdmin.put("avatarColor", admin.getAvatarColor());
        loginAdmin.put("status", admin.getStatus());
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_ADMIN_KEY + token,
                loginAdmin.toJSONString(), RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setAdmin(toAdminVO(admin));
        return vo;
    }

    @Override
    public void logout(String token) {
        stringRedisTemplate.delete(RedisConstants.LOGIN_ADMIN_KEY + token);
    }

    @Override
    public AdminVO getMe() {
        Admin admin = adminMapper.selectById(UserContext.getAdminId());
        if (admin == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return toAdminVO(admin);
    }

    @Override
    public WorkspaceVO workspace() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        WorkspaceVO vo = new WorkspaceVO();
        vo.setTodayNewUsers(Math.toIntExact(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, todayStart))));
        vo.setTodayOrders(Math.toIntExact(orderMapper.selectCount(new LambdaQueryWrapper<ProductOrder>()
                .ge(ProductOrder::getCreateTime, todayStart))));

        // 今日成交额（已支付未退款：状态 2/3/4）
        List<ProductOrder> todayPaid = orderMapper.selectList(new LambdaQueryWrapper<ProductOrder>()
                .ge(ProductOrder::getCreateTime, todayStart)
                .in(ProductOrder::getStatus, 2, 3, 4)
                .select(ProductOrder::getAmount));
        BigDecimal todayTurnover = todayPaid.stream()
                .map(ProductOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodayTurnover(todayTurnover);

        vo.setTotalUsers(Math.toIntExact(userMapper.selectCount(null)));
        vo.setTotalProducts(Math.toIntExact(productMapper.selectCount(null)));
        vo.setTotalOrders(Math.toIntExact(orderMapper.selectCount(null)));
        vo.setPendingAuditCount(Math.toIntExact(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 0))));
        vo.setPendingReportCount(Math.toIntExact(reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, 0))));
        return vo;
    }

    private AdminVO toAdminVO(Admin admin) {
        AdminVO vo = new AdminVO();
        BeanUtils.copyProperties(admin, vo);
        return vo;
    }
}
