const state = {
    accessToken: localStorage.getItem("adminAccessToken") || "",
    refreshToken: localStorage.getItem("adminRefreshToken") || "",
    admin: null,
    selectedUserId: null,
    selectedAdminId: null,
    page: 0,
    size: 20,
    keyword: "",
    searchType: "ALL",
    accountStatus: "ALL",
    auditPage: 0,
    auditSize: 30
};

const policyKeys = [
    "match.cooldownHours",
    "match.candidatePoolSize",
    "match.pickPoolSize",
    "ting.vipThreshold",
    "ting.cost.extraProfile",
    "ting.cost.extraProfileBundle5",
    "ting.cost.message",
    "ting.cost.like",
    "ting.cost.viewExtraPhoto",
    "ting.cost.viewScore",
    "ting.cost.viewLikedMeProfile",
    "ting.cost.viewHighScoreProfile",
    "benefit.basic.dailyProfile",
    "benefit.basic.dailyLoveView",
    "benefit.membership.cycleExtraProfiles",
    "benefit.membership.cycleFreeMessages",
    "benefit.membership.cycleFreeLikes",
    "benefit.vip.dailyExtraProfiles",
    "benefit.vip.dailyFreeMessages",
    "benefit.vip.dailyFreeLikes"
];

const roleOptions = [
    { value: "SUPER_ADMIN", name: "총괄 관리자", description: "관리자 계정, 정책, 회원, 지갑, 문의 전체 관리" },
    { value: "OPERATOR", name: "운영 관리자", description: "회원 상태, 푸시, 문의 처리" },
    { value: "SUPPORT", name: "고객지원", description: "회원 조회와 문의 답변" },
    { value: "MODERATOR", name: "심사 관리자", description: "회원/프로필 검토 중심" },
    { value: "FINANCE", name: "재무 관리자", description: "팅 지갑 조회와 조정" }
];

const policyLabels = {
    "match.cooldownHours": "추천 재요청 제한 시간",
    "match.candidatePoolSize": "추천 후보군 크기",
    "match.pickPoolSize": "추천 추첨 풀 크기",
    "ting.vipThreshold": "VIP 기준 팅 보유량",
    "ting.cost.extraProfile": "추가 프로필 1개 가격",
    "ting.cost.extraProfileBundle5": "추가 프로필 5개 묶음 가격",
    "ting.cost.message": "메시지 요청 가격",
    "ting.cost.like": "호감 요청 가격",
    "ting.cost.viewExtraPhoto": "추가 사진 열람 가격",
    "ting.cost.viewScore": "점수 조회 가격",
    "ting.cost.viewLikedMeProfile": "나를 호감한 상대 보기 가격",
    "ting.cost.viewHighScoreProfile": "높은 점수 준 상대 보기 가격",
    "benefit.basic.dailyProfile": "일반 회원 일일 프로필 추천권",
    "benefit.basic.dailyLoveView": "일반 회원 일일 연애관 추천권",
    "benefit.membership.cycleExtraProfiles": "멤버십 주기별 추가 프로필",
    "benefit.membership.cycleFreeMessages": "멤버십 주기별 무료 메시지",
    "benefit.membership.cycleFreeLikes": "멤버십 주기별 무료 호감",
    "benefit.vip.dailyExtraProfiles": "VIP 일일 추가 프로필",
    "benefit.vip.dailyFreeMessages": "VIP 일일 무료 메시지",
    "benefit.vip.dailyFreeLikes": "VIP 일일 무료 호감"
};

const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    if (state.accessToken) {
        showAdmin().catch(() => showLogin());
    } else {
        showLogin();
    }
});

function bindEvents() {
    $("loginForm").addEventListener("submit", login);
    $("logoutButton").addEventListener("click", logout);
    $("refreshButton").addEventListener("click", refreshCurrentView);
    $("userSearchButton").addEventListener("click", () => {
        applyUserFilters();
    });
    $("userKeyword").addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            applyUserFilters();
        }
    });
    $("userSearchType").addEventListener("change", applyUserFilters);
    $("userStatusFilter").addEventListener("change", applyUserFilters);
    $("userPageSize").addEventListener("change", () => {
        state.size = numberOrZero($("userPageSize").value) || 20;
        state.page = 0;
        loadUsers();
    });
    $("clearUserFilterButton").addEventListener("click", clearUserFilters);
    $("prevPageButton").addEventListener("click", () => {
        if (state.page > 0) {
            state.page -= 1;
            loadUsers();
        }
    });
    $("nextPageButton").addEventListener("click", () => {
        state.page += 1;
        loadUsers();
    });
    $("pushForm").addEventListener("submit", sendPush);
    $("pushTargetType").addEventListener("change", syncPushTargetInput);
    $("adminForm").addEventListener("submit", saveAdminAccount);
    $("newAdminButton").addEventListener("click", resetAdminForm);
    $("deleteAdminButton").addEventListener("click", deleteAdminAccount);
    $("inquiryStatusFilter").addEventListener("change", loadInquiries);
    $("auditPageSize").addEventListener("change", () => {
        state.auditSize = numberOrZero($("auditPageSize").value) || 30;
        state.auditPage = 0;
        loadAudits();
    });
    $("prevAuditPageButton").addEventListener("click", () => {
        if (state.auditPage > 0) {
            state.auditPage -= 1;
            loadAudits();
        }
    });
    $("nextAuditPageButton").addEventListener("click", () => {
        state.auditPage += 1;
        loadAudits();
    });
    renderRoleCards([]);
    document.querySelectorAll(".nav-item").forEach((button) => {
        button.addEventListener("click", () => switchView(button.dataset.view));
    });
}

async function login(event) {
    event.preventDefault();
    $("loginError").textContent = "";
    try {
        const response = await request("/api/admin/auth/login", {
            method: "POST",
            auth: false,
            body: {
                loginId: $("loginIdInput").value.trim(),
                password: $("passwordInput").value
            }
        });
        saveTokens(response);
        await showAdmin();
    } catch (error) {
        $("loginError").textContent = error.message;
    }
}

async function logout() {
    try {
        if (state.refreshToken) {
            await request("/api/admin/auth/logout", {
                method: "POST",
                body: { refreshToken: state.refreshToken }
            });
        }
    } catch (error) {
        console.warn(error);
    }
    clearTokens();
    showLogin();
}

async function showAdmin() {
    state.admin = await request("/api/admin/auth/me");
    $("adminName").textContent = `${state.admin.name} · ${(state.admin.roles || []).join(", ")}`;
    document.querySelectorAll(".super-only").forEach((element) => {
        element.classList.toggle("hidden", !(state.admin.roles || []).includes("SUPER_ADMIN"));
    });
    $("loginView").classList.add("hidden");
    $("adminView").classList.remove("hidden");
    await loadUsers();
}

function showLogin() {
    $("loginView").classList.remove("hidden");
    $("adminView").classList.add("hidden");
}

function saveTokens(response) {
    state.accessToken = response.accessToken;
    state.refreshToken = response.refreshToken;
    localStorage.setItem("adminAccessToken", state.accessToken);
    localStorage.setItem("adminRefreshToken", state.refreshToken);
}

function clearTokens() {
    state.accessToken = "";
    state.refreshToken = "";
    state.admin = null;
    localStorage.removeItem("adminAccessToken");
    localStorage.removeItem("adminRefreshToken");
}

function switchView(viewId) {
    document.querySelectorAll(".nav-item").forEach((button) => {
        button.classList.toggle("active", button.dataset.view === viewId);
    });
    document.querySelectorAll(".view").forEach((view) => {
        view.classList.toggle("hidden", view.id !== viewId);
    });
    const titles = {
        usersView: "회원 관리",
        policiesView: "운영 정책",
        pushView: "푸시 알림",
        inquiriesView: "문의",
        auditsView: "감사 로그",
        adminsView: "관리자 계정"
    };
    $("pageTitle").textContent = titles[viewId] || "회원 관리";
    refreshCurrentView();
}

function refreshCurrentView() {
    if (!$("auditsView").classList.contains("hidden")) {
        loadAudits();
    } else if (!$("inquiriesView").classList.contains("hidden")) {
        loadInquiries();
    } else if (!$("adminsView").classList.contains("hidden")) {
        loadAdminAccounts();
    } else if (!$("pushView").classList.contains("hidden")) {
        syncPushTargetInput();
    } else if (!$("policiesView").classList.contains("hidden")) {
        loadPolicies();
    } else {
        loadUsers();
        if (state.selectedUserId) {
            loadUserDetail(state.selectedUserId);
        }
    }
}

async function loadUsers() {
    const query = new URLSearchParams({
        page: String(state.page),
        size: String(state.size),
        searchType: state.searchType,
        accountStatus: state.accountStatus
    });
    if (state.keyword) {
        query.set("keyword", state.keyword);
    }
    const data = await request(`/api/admin/users?${query.toString()}`);
    renderUsers(data);
}

function applyUserFilters() {
    state.keyword = $("userKeyword").value.trim();
    state.searchType = $("userSearchType").value;
    state.accountStatus = $("userStatusFilter").value;
    state.page = 0;
    loadUsers();
}

function clearUserFilters() {
    $("userKeyword").value = "";
    $("userSearchType").value = "ALL";
    $("userStatusFilter").value = "ALL";
    $("userPageSize").value = "20";
    state.keyword = "";
    state.searchType = "ALL";
    state.accountStatus = "ALL";
    state.size = 20;
    state.page = 0;
    loadUsers();
}

function renderUsers(data) {
    $("userCount").textContent = `${data.totalCount.toLocaleString()}명`;
    $("pageInfo").textContent = `${data.page + 1} / ${Math.max(data.totalPages || 1, 1)} 페이지`;
    $("prevPageButton").disabled = data.page <= 0;
    $("nextPageButton").disabled = (data.page + 1) * data.size >= data.totalCount;
    $("usersTable").innerHTML = data.users.map((user) => `
        <tr data-user-id="${escapeHtml(user.userId)}">
            <td>${escapeHtml(user.userId)}</td>
            <td>${escapeHtml(user.profileId || "-")}</td>
            <td>${escapeHtml(user.nickName || user.userName || "-")}</td>
            <td>${escapeHtml(user.universityName || "-")}</td>
            <td>${escapeHtml([user.regionSidoName, user.regionSigunguName].filter(Boolean).join(" ") || "-")}</td>
            <td>${statusBadge(user.accountStatus)}</td>
        </tr>
    `).join("");
    document.querySelectorAll("tr[data-user-id]").forEach((row) => {
        row.addEventListener("click", () => loadUserDetail(row.dataset.userId));
    });
}

async function loadUserDetail(userId) {
    state.selectedUserId = userId;
    const user = await request(`/api/admin/users/${userId}`);
    renderUserDetail(user);
}

function renderUserDetail(user) {
    $("selectedUserLabel").textContent = `userId ${user.userId}`;
    $("userActionResult").textContent = "";
    $("userDetail").className = "detail-body";
    $("userDetail").innerHTML = `
        <section>
            <p class="section-title">기본 정보</p>
            ${kv("이름", user.userName)}
            ${kv("카카오 ID", user.kakaoId)}
            ${kv("가입일", formatDate(user.createdAt))}
            ${kv("인증", user.verified ? "완료" : "미완료")}
            ${kv("멤버십", user.membership ? "활성" : "비활성")}
            ${kv("멤버십 만료", formatDate(user.wallet?.membershipActiveUntil))}
            ${kv("계정 상태", statusBadge(user.accountStatus))}
            ${kv("상태 사유", user.statusReason || "-")}
        </section>
        <section>
            <div class="panel-head compact-head">
                <p class="section-title">운영 이력</p>
                <span id="userHistoryInfo" class="muted"></span>
            </div>
            <div id="userHistoryList" class="history-list">
                <span class="muted">이력을 불러오는 중입니다.</span>
            </div>
        </section>
        <section>
            <p class="section-title">프로필</p>
            ${kv("프로필 ID", user.profile?.profileId || "-")}
            ${kv("닉네임", user.profile?.nickName || "-")}
            ${kv("성별", genderLabel(user.profile?.gender))}
            ${kv("학교", user.profile?.universityName || "-")}
            ${kv("지역", [user.profile?.regionSidoName, user.profile?.regionSigunguName].filter(Boolean).join(" ") || "-")}
            ${kv("평점", user.profile?.grade ?? "-")}
            ${kv("키", user.profile?.height ?? "-")}
        </section>
        <section>
            <p class="section-title">지갑</p>
            ${kv("팅", user.wallet?.ting ?? "-")}
            ${kv("이벤트 팅", user.wallet?.eventTing ?? "-")}
        </section>
        <section class="action-box">
            <p class="section-title">멤버십 활성화</p>
            ${reasonControl("membershipReason", "멤버십 활성화 사유", ["결제 확인", "결제 보정", "이벤트 지급", "기타"])}
            <button id="membershipActivateButton" class="secondary">멤버십 활성화</button>
        </section>
        <section>
            <p class="section-title">사진</p>
            <div class="photo-grid">
                ${(user.photos || []).map((photo) => `
                    <a href="${escapeHtml(photo.url)}" target="_blank" rel="noreferrer" class="photo-tile">
                        <img src="${escapeHtml(photo.url)}" alt="profile photo">
                        <span>${photo.main ? "대표" : `#${escapeHtml(photo.imageIndex)}`}</span>
                    </a>
                `).join("") || `<span class="muted">등록된 사진 없음</span>`}
            </div>
        </section>
        <section class="action-box">
            <p class="section-title">계정 상태 변경</p>
            <select id="statusSelect">
                <option value="ACTIVE">ACTIVE</option>
                <option value="SUSPENDED">SUSPENDED</option>
                <option value="WITHDRAWN">WITHDRAWN</option>
            </select>
            ${reasonControl("statusReason", "상태 변경 사유", ["운영 정책 위반", "신고 누적", "본인 요청", "기타"])}
            <button id="statusSaveButton" class="secondary">상태 저장</button>
        </section>
        <section class="action-box">
            <p class="section-title">팅 조정</p>
            <div class="inline-grid">
                <input id="tingDelta" type="number" placeholder="팅 증감">
                <input id="eventTingDelta" type="number" placeholder="이벤트 팅 증감">
            </div>
            ${reasonControl("walletReason", "지갑 조정 사유", ["결제 보정", "이벤트 지급", "환불 처리", "기타"])}
            <button id="walletSaveButton" class="secondary">지갑 반영</button>
        </section>
    `;
    $("statusSelect").value = user.accountStatus || "ACTIVE";
    $("statusSaveButton").addEventListener("click", saveUserStatus);
    $("walletSaveButton").addEventListener("click", saveWallet);
    $("membershipActivateButton").addEventListener("click", activateMembership);
    bindReasonControl("statusReason", user.statusReason || "");
    bindReasonControl("walletReason", "");
    bindReasonControl("membershipReason", "");
    loadUserHistory(user.userId);
}

async function saveUserStatus() {
    if (!state.selectedUserId) return;
    try {
        await request(`/api/admin/users/${state.selectedUserId}/status`, {
            method: "PATCH",
            body: {
                status: $("statusSelect").value,
                reason: getReasonValue("statusReason")
            }
        });
        await loadUserDetail(state.selectedUserId);
        await loadUsers();
        showUserActionResult("상태 변경이 반영되었습니다.");
    } catch (error) {
        showUserActionResult(`상태 변경 실패: ${error.message}`, true);
    }
}

async function saveWallet() {
    if (!state.selectedUserId) return;
    await request(`/api/admin/users/${state.selectedUserId}/wallet`, {
        method: "PATCH",
        body: {
            tingDelta: numberOrZero($("tingDelta").value),
            eventTingDelta: numberOrZero($("eventTingDelta").value),
            reason: getReasonValue("walletReason")
        }
    });
    $("tingDelta").value = "";
    $("eventTingDelta").value = "";
    $("walletReasonSelect").value = "결제 보정";
    $("walletReasonOther").value = "";
    $("walletReasonOther").classList.add("hidden");
    await loadUserDetail(state.selectedUserId);
    showUserActionResult("지갑 조정이 반영되었습니다.");
}

async function activateMembership() {
    if (!state.selectedUserId) return;
    try {
        await request(`/api/admin/users/${state.selectedUserId}/wallet/membership`, {
            method: "POST",
            body: {
                reason: getReasonValue("membershipReason")
            }
        });
        $("membershipReasonSelect").value = "결제 확인";
        $("membershipReasonOther").value = "";
        $("membershipReasonOther").classList.add("hidden");
        await loadUserDetail(state.selectedUserId);
        await loadUsers();
        showUserActionResult("멤버십이 활성화되었습니다.");
    } catch (error) {
        showUserActionResult(`멤버십 활성화 실패: ${error.message}`, true);
    }
}

function showUserActionResult(message, error = false) {
    $("userActionResult").textContent = message;
    $("userActionResult").classList.toggle("error-text", error);
}

async function loadPolicies() {
    const data = await request("/api/admin/policies");
    renderPolicies(flattenPolicy(data));
}

function renderPolicies(items) {
    $("policyList").innerHTML = items.map(({ key, value }) => `
        <div class="policy-row" data-policy-key="${escapeHtml(key)}">
            <div class="policy-key">
                <strong>${escapeHtml(policyLabels[key] || key)}</strong>
                <small>${escapeHtml(key)}</small>
            </div>
            <input type="number" value="${escapeHtml(value)}" data-policy-value>
            ${reasonInline("policy", ["정책 조정", "이벤트 대응", "운영 테스트", "기타"])}
            <div class="inline-grid">
                <button class="secondary" data-policy-save>저장</button>
                <button class="ghost" data-policy-reset>초기화</button>
            </div>
        </div>
    `).join("");
    document.querySelectorAll("[data-policy-save]").forEach((button) => {
        button.addEventListener("click", () => savePolicy(button.closest(".policy-row"), false));
    });
    document.querySelectorAll("[data-policy-reset]").forEach((button) => {
        button.addEventListener("click", () => savePolicy(button.closest(".policy-row"), true));
    });
    document.querySelectorAll("[data-inline-reason-select]").forEach((select) => {
        select.addEventListener("change", () => {
            select.closest(".reason-inline").querySelector("[data-inline-reason-other]")
                    .classList.toggle("hidden", select.value !== "기타");
        });
    });
}

async function savePolicy(row, resetToDefault) {
    const key = row.dataset.policyKey;
    const reason = getInlineReason(row);
    const value = numberOrZero(row.querySelector("[data-policy-value]").value);
    await request("/api/admin/policies", {
        method: "PATCH",
        body: { key, value, resetToDefault, reason }
    });
    await loadPolicies();
}

function syncPushTargetInput() {
    const isAll = $("pushTargetType").value === "ALL";
    $("pushUserId").disabled = isAll;
    if (isAll) {
        $("pushUserId").value = "";
    }
}

async function sendPush(event) {
    event.preventDefault();
    $("pushResult").textContent = "";
    const targetType = $("pushTargetType").value;
    if (targetType === "ALL" && !confirm("전체 활성 디바이스 토큰으로 푸시를 발송할까요?")) {
        return;
    }

    const response = await request("/api/admin/push", {
        method: "POST",
        body: {
            targetType,
            userId: targetType === "USER" ? numberOrNull($("pushUserId").value) : null,
            title: $("pushTitle").value.trim(),
            body: $("pushBody").value.trim(),
            reason: $("pushReason").value.trim()
        }
    });
    $("pushResult").textContent =
        `대상 토큰 ${response.targetTokenCount}개, 성공 ${response.successCount}개, 실패 ${response.failureCount}개, 비활성화 ${response.invalidTokenCount}개`;
}

async function loadAdminAccounts() {
    if (!(state.admin?.roles || []).includes("SUPER_ADMIN")) {
        return;
    }
    const admins = await request("/api/admin/accounts");
    renderAdminAccounts(admins);
}

function renderAdminAccounts(admins) {
    $("adminsTable").innerHTML = admins.map((admin) => `
        <tr data-admin-id="${escapeHtml(admin.adminId)}">
            <td>${escapeHtml(admin.adminId)}</td>
            <td>${escapeHtml(admin.loginId)}</td>
            <td>${escapeHtml(admin.name)}</td>
            <td>${escapeHtml((admin.roles || []).join(", "))}</td>
            <td>${statusBadge(admin.status)}</td>
        </tr>
    `).join("");
    document.querySelectorAll("tr[data-admin-id]").forEach((row) => {
        row.addEventListener("click", () => {
            const admin = admins.find((item) => String(item.adminId) === row.dataset.adminId);
            fillAdminForm(admin);
        });
    });
}

function fillAdminForm(admin) {
    state.selectedAdminId = admin.adminId;
    $("selectedAdminLabel").textContent = `adminId ${admin.adminId}`;
    $("adminIdInput").value = admin.adminId;
    $("adminLoginIdInput").value = admin.loginId;
    $("adminLoginIdInput").disabled = true;
    $("adminNameInput").value = admin.name;
    renderRoleCards(admin.roles || []);
    $("adminStatusInput").value = admin.status;
    $("adminPasswordInput").value = "";
    $("deleteAdminButton").classList.remove("hidden");
    $("adminFormResult").textContent = "";
}

function resetAdminForm() {
    state.selectedAdminId = null;
    $("selectedAdminLabel").textContent = "신규";
    $("adminIdInput").value = "";
    $("adminLoginIdInput").value = "";
    $("adminLoginIdInput").disabled = false;
    $("adminNameInput").value = "";
    renderRoleCards(["OPERATOR"]);
    $("adminStatusInput").value = "ACTIVE";
    $("adminPasswordInput").value = "";
    $("deleteAdminButton").classList.add("hidden");
    $("adminFormResult").textContent = "";
}

async function saveAdminAccount(event) {
    event.preventDefault();
    $("adminFormResult").textContent = "";
    const adminId = $("adminIdInput").value;
    try {
        const successMessage = adminId ? "관리자 계정을 수정했습니다." : "관리자 계정을 생성했습니다.";
        if (!adminId) {
            await request("/api/admin/accounts", {
                method: "POST",
                body: {
                    loginId: $("adminLoginIdInput").value.trim(),
                    password: $("adminPasswordInput").value,
                    name: $("adminNameInput").value.trim(),
                    roles: selectedRoles()
                }
            });
        } else {
            await request(`/api/admin/accounts/${adminId}`, {
                method: "PATCH",
                body: {
                    name: $("adminNameInput").value.trim(),
                    roles: selectedRoles(),
                    status: $("adminStatusInput").value
                }
            });
            if ($("adminPasswordInput").value) {
                await request(`/api/admin/accounts/${adminId}/password`, {
                    method: "PATCH",
                    body: { password: $("adminPasswordInput").value }
                });
            }
        }
        resetAdminForm();
        await loadAdminAccounts();
        $("adminFormResult").textContent = successMessage;
    } catch (error) {
        $("adminFormResult").textContent = `저장 실패: ${error.message}`;
    }
}

async function loadInquiries() {
    const status = $("inquiryStatusFilter").value;
    const query = status ? `?status=${encodeURIComponent(status)}` : "";
    const inquiries = await request(`/api/admin/inquiries${query}`);
    $("inquiryList").innerHTML = inquiries.map((inquiry) => `
        <button type="button" class="list-item" data-inquiry-id="${escapeHtml(inquiry.inquiryId)}">
            <strong>${escapeHtml(inquiry.title)}</strong>
            <span>${escapeHtml(inquiry.category)} · ${escapeHtml(inquiry.status)} · userId ${escapeHtml(inquiry.userId)}</span>
        </button>
    `).join("") || `<div class="detail-empty">문의가 없습니다.</div>`;
    document.querySelectorAll("[data-inquiry-id]").forEach((button) => {
        button.addEventListener("click", () => loadInquiryDetail(button.dataset.inquiryId));
    });
}

async function loadInquiryDetail(inquiryId) {
    const inquiry = await request(`/api/admin/inquiries/${inquiryId}`);
    $("selectedInquiryLabel").textContent = `문의 ${inquiry.inquiryId}`;
    $("inquiryDetail").className = "detail-body";
    $("inquiryDetail").innerHTML = `
        <section>
            ${kv("회원", inquiry.userId)}
            ${kv("카테고리", inquiry.category)}
            ${kv("상태", inquiry.status)}
            ${kv("제목", inquiry.title)}
            ${kv("내용", inquiry.content)}
            ${kv("생성일", formatDate(inquiry.createdAt))}
        </section>
        <section>
            <p class="section-title">관련 정보</p>
            <pre class="context-box">${escapeHtml(JSON.stringify(inquiry.relatedContext || {}, null, 2))}</pre>
        </section>
        <section class="action-box">
            <p class="section-title">답변</p>
            <textarea id="inquiryAnswer">${escapeHtml(inquiry.answer || "")}</textarea>
            <button id="answerInquiryButton" class="secondary">답변 저장</button>
            <p id="inquiryAnswerResult" class="muted"></p>
        </section>
    `;
    $("answerInquiryButton").addEventListener("click", async () => {
        try {
            await request(`/api/admin/inquiries/${inquiry.inquiryId}/answer`, {
                method: "PATCH",
                body: { answer: $("inquiryAnswer").value.trim() }
            });
            $("inquiryAnswerResult").textContent = "답변이 저장되었습니다.";
            await loadInquiries();
        } catch (error) {
            $("inquiryAnswerResult").textContent = `답변 실패: ${error.message}`;
        }
    });
}

async function loadAudits() {
    const query = new URLSearchParams({
        page: String(state.auditPage),
        size: String(state.auditSize)
    });
    const data = await request(`/api/admin/audits?${query.toString()}`);
    renderAudits(data);
}

function renderAudits(data) {
    $("auditPageInfo").textContent =
        `${data.totalCount.toLocaleString()}건 · ${data.page + 1} / ${Math.max(data.totalPages || 1, 1)} 페이지`;
    $("prevAuditPageButton").disabled = data.page <= 0;
    $("nextAuditPageButton").disabled = (data.page + 1) * data.size >= data.totalCount;
    $("auditsTable").innerHTML = data.logs.map((log) => `
        <tr>
            <td>${escapeHtml(formatDate(log.createdAt))}</td>
            <td>${escapeHtml(adminActorLabel(log))}</td>
            <td>${escapeHtml(auditActionLabel(log.actionType))}</td>
            <td>${escapeHtml(`${auditTargetLabel(log.targetType)} ${log.targetId ?? "-"}`)}</td>
            <td class="compact-text">${escapeHtml(changeLabel(log))}</td>
            <td class="compact-text">${escapeHtml(log.reason || "-")}</td>
            <td>${escapeHtml(log.ipAddress || "-")}</td>
        </tr>
    `).join("") || `<tr><td colspan="7" class="muted">감사 로그가 없습니다.</td></tr>`;
}

async function loadUserHistory(userId) {
    try {
        const data = await request(`/api/admin/audits/users/${userId}?page=0&size=8`);
        $("userHistoryInfo").textContent = `${data.totalCount.toLocaleString()}건`;
        $("userHistoryList").innerHTML = data.logs.map((log) => `
            <div class="history-item">
                <strong>${escapeHtml(auditActionLabel(log.actionType))}</strong>
                <span>${escapeHtml(formatDate(log.createdAt))} · ${escapeHtml(adminActorLabel(log))}</span>
                <small>${escapeHtml(changeLabel(log))}</small>
                ${log.reason ? `<small>사유: ${escapeHtml(log.reason)}</small>` : ""}
            </div>
        `).join("") || `<span class="muted">운영 이력이 없습니다.</span>`;
    } catch (error) {
        $("userHistoryInfo").textContent = "";
        $("userHistoryList").innerHTML = `<span class="error-text">운영 이력 조회 실패: ${escapeHtml(error.message)}</span>`;
    }
}

async function deleteAdminAccount() {
    const adminId = $("adminIdInput").value;
    if (!adminId || !confirm("선택한 관리자 계정을 삭제할까요?")) return;
    try {
        await request(`/api/admin/accounts/${adminId}`, { method: "DELETE" });
        resetAdminForm();
        await loadAdminAccounts();
        $("adminFormResult").textContent = "관리자 계정을 삭제했습니다.";
    } catch (error) {
        $("adminFormResult").textContent = `삭제 실패: ${error.message}`;
    }
}

function renderRoleCards(selected) {
    const selectedSet = new Set(selected);
    $("adminRoleCards").innerHTML = roleOptions.map((role) => `
        <button type="button" class="role-card ${selectedSet.has(role.value) ? "selected" : ""}" data-role="${role.value}">
            <strong>${escapeHtml(role.name)}</strong>
            <span>${escapeHtml(role.value)}</span>
            <small>${escapeHtml(role.description)}</small>
        </button>
    `).join("");
    document.querySelectorAll("[data-role]").forEach((button) => {
        button.addEventListener("click", () => button.classList.toggle("selected"));
    });
}

function selectedRoles() {
    return [...document.querySelectorAll(".role-card.selected")].map((button) => button.dataset.role);
}

function flattenPolicy(policy) {
    return policyKeys.map((key) => ({ key, value: getNested(policy, key) ?? 0 }));
}

function getNested(source, dottedKey) {
    return dottedKey.split(".").reduce((current, key) => current?.[key], source);
}

function reasonControl(id, label, options) {
    return `
        <label>
            <span>${escapeHtml(label)}</span>
            <select id="${id}Select" data-reason-select="${id}">
                ${options.map((option) => `<option value="${escapeHtml(option)}">${escapeHtml(option)}</option>`).join("")}
            </select>
        </label>
        <input id="${id}Other" class="hidden" type="text" placeholder="기타 사유 입력">
    `;
}

function bindReasonControl(id, initialValue) {
    const select = $(`${id}Select`);
    const other = $(`${id}Other`);
    if (initialValue && [...select.options].every((option) => option.value !== initialValue)) {
        select.value = "기타";
        other.value = initialValue;
        other.classList.remove("hidden");
    }
    select.addEventListener("change", () => {
        other.classList.toggle("hidden", select.value !== "기타");
    });
}

function getReasonValue(id) {
    const selected = $(`${id}Select`).value;
    if (selected === "기타") {
        return $(`${id}Other`).value.trim();
    }
    return selected;
}

function reasonInline(prefix, options) {
    return `
        <div class="reason-inline">
            <select data-inline-reason-select>
                ${options.map((option) => `<option value="${escapeHtml(option)}">${escapeHtml(option)}</option>`).join("")}
            </select>
            <input class="hidden" data-inline-reason-other type="text" placeholder="기타 사유">
        </div>
    `;
}

function getInlineReason(row) {
    const select = row.querySelector("[data-inline-reason-select]");
    const other = row.querySelector("[data-inline-reason-other]");
    return select.value === "기타" ? other.value.trim() : select.value;
}

async function request(url, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body !== undefined) {
        headers["Content-Type"] = "application/json";
    }
    if (options.auth !== false && state.accessToken) {
        headers.Authorization = `Bearer ${state.accessToken}`;
    }

    const response = await fetch(url, {
        method: options.method || "GET",
        headers,
        body: options.body !== undefined ? JSON.stringify(options.body) : undefined
    });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data?.message || "요청 처리에 실패했습니다.");
    }
    return data;
}

function kv(label, value) {
    return `<div class="kv"><span>${escapeHtml(label)}</span><span>${value === undefined || value === null ? "-" : value}</span></div>`;
}

function statusBadge(status) {
    const lower = String(status || "ACTIVE").toLowerCase();
    return `<span class="badge ${escapeHtml(lower)}">${escapeHtml(status || "ACTIVE")}</span>`;
}

function genderLabel(gender) {
    const labels = {
        MALE: "남성",
        FEMALE: "여성"
    };
    return labels[gender] || gender || "-";
}

function adminActorLabel(log) {
    return `${log.adminName || "관리자"}(${log.adminLoginId || log.adminId || "-"})`;
}

function auditActionLabel(actionType) {
    const labels = {
        ADMIN_LOGIN: "관리자 로그인",
        ADMIN_LOGOUT: "관리자 로그아웃",
        ADMIN_REFRESH: "토큰 갱신",
        ADMIN_ACCOUNT_CREATE: "관리자 생성",
        ADMIN_ACCOUNT_UPDATE: "관리자 수정",
        ADMIN_ACCOUNT_DELETE: "관리자 삭제",
        ADMIN_PASSWORD_RESET: "비밀번호 재설정",
        USER_STATUS_UPDATE: "회원 상태 변경",
        WALLET_ADJUST: "팅 지갑 조정",
        MEMBERSHIP_ACTIVATE: "멤버십 활성화",
        POLICY_UPDATE: "운영 정책 변경",
        PUSH_SEND: "푸시 발송"
    };
    return labels[actionType] || actionType || "-";
}

function auditTargetLabel(targetType) {
    const labels = {
        ADMIN: "관리자",
        USER: "회원",
        TING_WALLET: "팅 지갑",
        POLICY: "운영 정책",
        PUSH: "푸시"
    };
    return labels[targetType] || targetType || "-";
}

function changeLabel(log) {
    if (!log.beforeValue && !log.afterValue) {
        return "-";
    }
    return `${log.beforeValue || "-"} -> ${log.afterValue || "-"}`;
}

function numberOrZero(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
}

function numberOrNull(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) && value !== "" ? parsed : null;
}

function formatDate(value) {
    if (!value) return "-";
    return new Date(value).toLocaleString("ko-KR");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
