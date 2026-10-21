package com.campus.service;

import com.campus.common.context.UserContext;
import com.campus.common.exception.BusinessException;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReviewMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.dto.OrderSubmitDTO;
import com.campus.pojo.entity.ProductOrder;
import com.campus.pojo.vo.OrderVO;
import com.campus.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceIdempotencyTest {

    private OrderMapper orderMapper;
    private ProductMapper productMapper;
    private RedissonClient redissonClient;
    private RLock lock;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        orderMapper = mock(OrderMapper.class);
        productMapper = mock(ProductMapper.class);
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);

        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "userMapper", mock(UserMapper.class));
        ReflectionTestUtils.setField(service, "reviewMapper", mock(ReviewMapper.class));
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);

        when(redissonClient.getLock("lock:order:10")).thenReturn(lock);
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        UserContext.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUserId();
    }

    @Test
    void retryWithSameRequestIdShouldReturnExistingOrder() {
        ProductOrder existing = new ProductOrder();
        existing.setId(99L);
        existing.setOrderNo("20261021000000000099");
        existing.setRequestId("request-1234567890");
        existing.setProductId(10L);
        existing.setBuyerId(7L);
        existing.setSellerId(8L);
        existing.setAmount(new BigDecimal("25.00"));
        existing.setStatus(1);
        when(orderMapper.selectByIdempotencyKeyForUpdate(7L, 10L, "request-1234567890"))
                .thenReturn(existing);

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setProductId(10L);
        dto.setRequestId("request-1234567890");
        OrderVO result = service.submit(dto);

        assertEquals(99L, result.getId());
        assertEquals(existing.getOrderNo(), result.getOrderNo());
        verify(productMapper, never()).casOnSaleToTrading(anyLong());
        verify(orderMapper, never()).insert(any(ProductOrder.class));
        verify(lock).unlock();
    }

    @Test
    void missingRequestIdShouldBeRejectedBeforeTakingLock() {
        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setProductId(10L);

        assertThrows(BusinessException.class, () -> service.submit(dto));

        verify(redissonClient, never()).getLock(any());
    }
}
