# 专栏真实响应夹具

2026-10-10 从已登录会话只读采集，使用项目 `ZhihuApiEnvironment.fetchJson` 的签名请求验证相同接口。

- `detail.json`：`GET https://www.zhihu.com/api/v4/columns/c_2085111823087547085`，include 为 `intro,description,articles_count,followers,author,is_following,voteup_count`。
- `page0.json` / `page1.json`：`GET https://www.zhihu.com/api/v4/columns/c_2085111823087547085/articles?limit=1&offset=0` 及服务端 next 指向的 offset=1。小页采样仅减少夹具条数；生产首屏为 limit=10，客户端必须尊重响应 next，而不能重新计算 offset/limit。
- `contributions.json`：`GET https://www.zhihu.com/api/v4/members/L.M.Sherlock/column-contributions`，include 为 `data[*].column.articles_count,followers,author,intro`。
- `contributions-missing-counts.json`：同一请求的旧 include `data[*].articles_count,followers,author`，原响应缺少嵌套专栏统计字段，用于复现全部显示 0 的问题。

仅一致性脱敏 author 中的身份字段（id、uid、name、url、url_token、avatar_url/template、headline）。没有改写正文 HTML、摘要、字段缺失形态、业务 id、计数与分页。原始响应、散列及逐字段脱敏审计保存在仓库外的私有证据目录；夹具不含会话凭据。
