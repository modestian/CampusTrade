/**
 * 校园二手交易平台 - 学生端共享脚本
 * 统一 axios 封装 / token 管理 / 工具函数
 */

const TOKEN_KEY = 'campus_token';
const USER_KEY = 'campus_user';

/** axios 实例：自动携带 token、统一处理响应 */
const api = axios.create({ timeout: 15000 });

api.interceptors.request.use(config => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
        config.headers['token'] = token;
    }
    return config;
});

api.interceptors.response.use(resp => {
    const r = resp.data;
    if (r.code === 1) {
        return r.data;
    }
    if (r.code === 401) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        alert('登录已过期，请重新登录');
        location.href = '/student/login.html';
        return Promise.reject(r);
    }
    alert(r.msg || '操作失败');
    return Promise.reject(r);
}, err => {
    alert('网络异常，请稍后重试');
    return Promise.reject(err);
});

/* ---------- 登录态 ---------- */

function isLogin() {
    return !!localStorage.getItem(TOKEN_KEY);
}

function currentUser() {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY) || '{}');
    } catch (e) {
        return {};
    }
}

function saveLogin(data) {
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
}

function logout() {
    api.post('/student/logout').finally(() => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        location.href = '/student/login.html';
    });
}

/* ---------- 展示工具 ---------- */

/** 色块样式（商品封面/头像） */
function colorStyle(color) {
    return { backgroundColor: color || '#DFE6E9' };
}

/** 取首字（色块内文字） */
function firstChars(text, len) {
    return text ? text.substring(0, len || 1) : '?';
}

/** 成色文案 */
function conditionText(level) {
    const arr = ['全新', '几乎全新', '轻微使用痕迹', '明显使用痕迹', '磨损明显'];
    return arr[(level || 3) - 1];
}

/** 订单状态文案 */
function orderStatusText(status) {
    const map = { 1: '待支付', 2: '待发货', 3: '待收货', 4: '已完成', 5: '已取消', 6: '退款中', 7: '已退款' };
    return map[status] || '未知';
}

/** 商品状态文案 */
function productStatusText(status) {
    const map = { 0: '待审核', 1: '在售', 2: '交易中', 3: '已售出', 4: '已下架', 5: '已驳回' };
    return map[status] || '未知';
}

/** 交付方式文案 */
function deliveryText(type) {
    return type === 2 ? '快递代取' : '校内面交';
}

/* ---------- 时间格式化 ---------- */
function fmtTime(time) {
    if (!time) return '';
    return String(time).replace('T', ' ').substring(0, 16);
}

/* ---------- 需要登录的页面守卫 ---------- */
function requireLogin() {
    if (!isLogin()) {
        location.href = '/student/login.html';
        return false;
    }
    return true;
}

/* ---------- 将展示工具函数注册到所有 Vue 实例 ----------
 * Vue 模板表达式只在实例作用域内求值：未注册进 methods 的全局函数
 * 在开发版渲染代理下无法解析（TypeError: xxx is not a function），
 * 会导致渲染中断、根元素被空节点替换（页面空白）。
 * 这里用全局 mixin 统一注入，页面模板即可直接调用如 {{ fmtTime(x) }}。
 * 注意：组件自身同名 methods 优先于 mixin，互不冲突。
 */
Vue.mixin({
    methods: {
        colorStyle,
        firstChars,
        conditionText,
        orderStatusText,
        productStatusText,
        deliveryText,
        fmtTime,
        logout,

        /* 智能返回：来源是站内页面时用浏览器历史返回（保留滚动位置）；
         * 直接打开/外部进入（无站内来源）时跳到 fallback 页面，避免返回无效。 */
        goBack(fallback) {
            if (document.referrer && document.referrer.indexOf(location.origin) === 0) {
                history.back();
            } else {
                location.href = fallback || '/student/index.html';
            }
        }
    },
    computed: {
        /* 让模板中可直接使用 location（如 @click="location.href='...'"）：
         * location 不在 Vue 允许的全局标识符列表中，模板直接引用会解析失败
         * 导致点击无响应（TypeError）。此处映射到 window.location，
         * 模板赋值 location.href 即正常跳转。 */
        location() {
            return window.location;
        }
    }
});
