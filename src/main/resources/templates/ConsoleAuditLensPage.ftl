<div class="cal-page">
    <style>
        .cal-page {
            width: 100%;
            margin: 0;
            color: var(--console-default-font-color, #222);
            font-size: inherit;
        }
        .cal-header {
            margin-bottom: 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid #e4e7eb;
        }
        #main-title {
            font-size: 1.2rem;
            font-weight: bold;
            margin-top: 10px;
            color: var(--console-default-font-color, #222);
            height: 23px;
        }
        .cal-subtitle {
            margin-top: 6px;
            color: var(--console-default-font-color, #555);
        }
        .cal-section {
            border-top: 1px solid #e4e7eb;
            padding-top: 12px;
            margin-top: 12px;
        }
        .cal-form {
            display: grid;
            grid-template-columns: 2fr 1fr 1fr 1fr 1fr 1fr auto;
            gap: 8px;
            align-items: end;
        }
        .cal-actions {
            display: flex;
            gap: 8px;
            align-items: center;
            justify-content: flex-end;
            white-space: nowrap;
        }
        .cal-form label {
            font-weight: 600;
            color: var(--console-default-font-color, #222);
            display: block;
            margin-bottom: 4px;
        }
        .cal-form input,
        .cal-form select {
            width: 100%;
            box-sizing: border-box;
            min-height: 36px;
        }
        .cal-btn {
            text-decoration: none;
            display: inline-block;
            white-space: nowrap;
        }
        .cal-btn.secondary {
            background: #556b84;
            border-color: #556b84;
            color: #fff;
        }
        .cal-meta-row {
            margin-top: 8px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;
        }
        .cal-meta {
            color: var(--console-default-font-color, #444);
        }
        .cal-table-wrap {
            overflow: auto;
            border: 1px solid #e4e7eb;
            border-radius: 8px;
            background: #fff;
        }
        .cal-table {
            width: 100%;
            border-collapse: collapse;
        }
        .cal-table th,
        .cal-table td {
            border-bottom: 1px solid #e7ebf2;
            padding: 10px;
            text-align: left;
            vertical-align: top;
            color: var(--console-default-font-color, #222);
        }
        .cal-table th {
            font-weight: 600;
            background: #f3f6fa;
            position: sticky;
            top: 0;
            z-index: 1;
        }
        .cal-tag {
            font-size: 12px;
            border-radius: 999px;
            padding: 3px 9px;
            display: inline-block;
            border: 1px solid #d6dde8;
            background: #f5f7fa;
            color: #44546a;
        }
        .cal-setup { background: #eef4fb; color: #3f5876; border-color: #d6dfeb; }
        .cal-plugin { background: #eef4fb; color: #3f5876; border-color: #d6dfeb; }
        .cal-governance { background: #eef4fb; color: #3f5876; border-color: #d6dfeb; }
        .cal-other { background: #eef4fb; color: #3f5876; border-color: #d6dfeb; }
        .cal-pager {
            margin-top: 12px;
            display: flex;
            gap: 8px;
        }
        .cal-pager a {
            text-decoration: none;
            border: 1px solid #cfd7e3;
            border-radius: 8px;
            padding: 7px 12px;
            color: var(--console-default-font-color, #1c4f87);
            background: #fff;
            font-weight: 600;
        }
        @media (max-width: 1280px) {
            .cal-form { grid-template-columns: 1fr 1fr 1fr 1fr; }
        }
        @media (max-width: 780px) {
            .cal-form { grid-template-columns: 1fr 1fr; }
        }
    </style>

    <div class="cal-header">
        <div id="main-title">Console Audit Lens</div>
        <div class="cal-subtitle">Unified view of setup/plugin/governance changes with filtering and export.</div>
    </div>

    <div class="cal-section">
        <form method="get" action="${request.contextPath}/web/console/plugin/ConsoleAuditLensPage" class="cal-form">
            <div>
                <label>Keyword</label>
                <input class="form-control" type="text" name="search" value="${search!''}" placeholder="class, method, message" />
            </div>
            <div>
                <label>Username</label>
                <input class="form-control" type="text" name="username" value="${username!''}" placeholder="admin" />
            </div>
            <div>
                <label>Area</label>
                <select class="form-control" name="area">
                    <option value="" <#if (area!'') == ''>selected</#if>>All</option>
                    <option value="setup" <#if (area!'') == 'setup'>selected</#if>>Setup</option>
                    <option value="plugin" <#if (area!'') == 'plugin'>selected</#if>>Plugin</option>
                    <option value="governance" <#if (area!'') == 'governance'>selected</#if>>Governance</option>
                    <option value="other" <#if (area!'') == 'other'>selected</#if>>Other</option>
                </select>
            </div>
            <div>
                <label>Date From</label>
                <input class="form-control" type="date" name="dateFrom" value="${dateFrom!''}" />
            </div>
            <div>
                <label>Date To</label>
                <input class="form-control" type="date" name="dateTo" value="${dateTo!''}" />
            </div>
            <div>
                <label>Rows</label>
                <input class="form-control" type="number" min="10" max="500" name="rows" value="${rows!200}" />
            </div>
            <div class="cal-actions">
                <button type="submit" class="btn button cal-btn">Apply</button>
                <a class="btn button cal-btn secondary" href="${exportUrl}">Export CSV</a>
            </div>
        </form>
        <div class="cal-meta-row">
            <div class="cal-meta">Showing ${records?size} records (total matched: ${totalCount!0})</div>
        </div>
    </div>

    <div class="cal-section cal-table-wrap">
        <table class="cal-table table">
            <thead>
                <tr>
                    <th style="min-width:150px;">Timestamp</th>
                    <th style="min-width:100px;">User</th>
                    <th style="min-width:110px;">Area</th>
                    <th style="min-width:220px;">Class</th>
                    <th style="min-width:120px;">Method</th>
                    <th>Message</th>
                    <th style="min-width:90px;">App ID</th>
                </tr>
            </thead>
            <tbody>
                <#if records?size == 0>
                    <tr><td colspan="7">No audit records found for current filter.</td></tr>
                <#else>
                    <#list records as r>
                    <tr>
                        <td>${r.timestamp!''}</td>
                        <td>${r.username!''}</td>
                        <td>
                            <#assign areaClass = 'cal-other'>
                            <#if (r.area!'') == 'setup'><#assign areaClass = 'cal-setup'></#if>
                            <#if (r.area!'') == 'plugin'><#assign areaClass = 'cal-plugin'></#if>
                            <#if (r.area!'') == 'governance'><#assign areaClass = 'cal-governance'></#if>
                            <span class="cal-tag ${areaClass}">${r.area!''}</span>
                        </td>
                        <td>${r.clazz!''}</td>
                        <td>${r.method!''}</td>
                        <td>${r.message!''}</td>
                        <td>${r.appId!''}</td>
                    </tr>
                    </#list>
                </#if>
            </tbody>
        </table>

        <div class="cal-pager">
            <a href="${request.contextPath}/web/console/plugin/ConsoleAuditLensPage?search=${(search!'')?url('UTF-8')}&username=${(username!'')?url('UTF-8')}&area=${(area!'')?url('UTF-8')}&dateFrom=${(dateFrom!'')?url('UTF-8')}&dateTo=${(dateTo!'')?url('UTF-8')}&rows=${rows!200}&start=${prevStart!0}">Prev</a>
            <a href="${request.contextPath}/web/console/plugin/ConsoleAuditLensPage?search=${(search!'')?url('UTF-8')}&username=${(username!'')?url('UTF-8')}&area=${(area!'')?url('UTF-8')}&dateFrom=${(dateFrom!'')?url('UTF-8')}&dateTo=${(dateTo!'')?url('UTF-8')}&rows=${rows!200}&start=${nextStart!0}">Next</a>
        </div>
    </div>
</div>
