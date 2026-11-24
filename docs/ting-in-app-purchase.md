# 팅 인앱결제 (Apple App Store / Google Play)

팅 충전은 스토어 인앱결제(IAP)로만 판매한다. 기프티콘 결제(토스페이먼츠)는 별도 흐름이며 이 문서 범위가 아니다.

## 1. 스키마

```
payment          결제 공통 원장. provider(APPLE_IAP/GOOGLE_IAP/TOSS), purpose(TING_CHARGE/GIFTICON),
                 amount, currency, environment, status, 시각, 실패/환불 사유
store_payment    인앱결제 상세. payment_id 를 PK 로 공유(1:1).
                 store, store_product_id, purchase_key(멱등 키), transaction_id, quantity,
                 account_token, consume 상태/재시도, 스토어 원문 payload
ting_purchase    팅 지급 상세. payment_id 를 PK 로 공유(1:1).
                 ting_product_id, 지급량 스냅샷, 실제 지급량, 환불 회수량
ting_product     팅 상품 카탈로그. apple_product_id, google_product_id
```

- `purchase_key`: Apple 은 `transactionId`, Google 은 `purchaseToken`. `(store, purchase_key)` 유니크로 중복 지급을 막는다.
- 결제 상태: `VERIFYING → PAID → REFUNDED`, 또는 `VERIFYING → FAILED`. 스토어 통신 장애는 `VERIFYING` 을 유지하고 사유만 남기므로 앱이 같은 요청을 다시 보내면 이어서 처리된다.
- 팅 지급/회수는 기존 `ting_transaction` 원장에 `TING_PURCHASE` / `TING_PURCHASE_REFUND` 로 기록된다. 멱등 키는 `TING_PURCHASE_CREDIT_{paymentId}` 등.
- `gifticon_payment` 는 이번에 이관하지 않았다. 추후 `toss_payment` 상세 테이블을 만들어 `payment` 아래로 옮긴다.

## 2. API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/ting/products` | 활성 상품 목록 + `purchaseAccountToken` |
| POST | `/api/ting/purchases/verify` | 스토어 영수증 검증 후 팅 지급 (멱등) |
| GET | `/api/ting/purchases/{paymentId}` | 구매 1건 조회 |
| GET | `/api/ting/purchases?size=20` | 내 구매 내역 |
| POST | `/api/iap/apple/notifications` | App Store Server Notifications V2 수신 (인증 없음, JWS 서명 검증) |
| GET/POST/PUT | `/api/admin/ting-products` | 관리자 상품 관리 (SUPER_ADMIN, FINANCE) |

검증 요청 본문:

```json
{ "store": "APPLE",  "productCode": "TING_70", "transactionId": "2000000123456789" }
{ "store": "GOOGLE", "productCode": "TING_70", "purchaseToken": "hgkfj...."}
```

응답 상태 코드:

- 200: `status` 가 `PAID` 이면 지급 완료. `tingBalance`, `eventTingBalance` 포함.
- 400: 검증 거절(상품 불일치, 위조, 환불된 구매, 결제 미완료). 메시지에 사유가 들어 있다.
- 502: 스토어 통신 장애. 같은 요청을 잠시 후 다시 보내면 된다.

## 3. 클라이언트 흐름

### iOS (StoreKit 2)

1. `GET /api/ting/products` 로 `appleProductId` 와 `purchaseAccountToken` 을 받는다.
2. `Product.purchase(options: [.appAccountToken(UUID(uuidString: purchaseAccountToken)!)])`
3. 결과가 `.success(.verified(transaction))` 이면 `transaction.id` 를 `transactionId` 로 서버에 보낸다.
4. 서버가 200 `PAID` 를 돌려주면 `await transaction.finish()`. 서버 오류(5xx)면 finish 하지 말고 재시도한다.
5. 앱 시작 시 `Transaction.unfinished` 를 순회해 3~4 를 반복하면 네트워크 끊김으로 놓친 지급이 복구된다.

### Android (Play Billing Library 7+)

1. `GET /api/ting/products` 로 `googleProductId` 와 `purchaseAccountToken` 을 받는다.
2. `BillingFlowParams.Builder().setObfuscatedAccountId(purchaseAccountToken)` 로 결제한다.
3. `Purchase.purchaseState == PURCHASED` 이면 `purchase.purchaseToken` 을 서버에 보낸다.
4. consume 은 서버가 `purchases.products.consume` 으로 처리한다. 앱에서 `consumeAsync` 를 부를 필요가 없다.
5. 앱 시작 시 `queryPurchasesAsync` 로 남아 있는 구매를 3 으로 다시 보내면 놓친 지급이 복구된다.

`purchaseAccountToken` 은 서버가 userId 로부터 결정적으로 만든 UUID 다. 스토어 영수증에 실려 돌아오면 서버가 요청 사용자와 비교하므로, 다른 계정의 영수증을 가져와 지급받는 시도를 막는다. 모든 앱 버전이 토큰을 넣게 된 뒤 `TING_PURCHASE_REQUIRE_ACCOUNT_TOKEN=true` 로 바꾼다.

## 4. App Store Connect 신청 절차

1. **계약/세금/은행**: App Store Connect > 비즈니스(Business) 에서 유료 앱 계약(Paid Apps Agreement)에 동의하고 세금·은행 정보를 입력한다. 이게 없으면 인앱 구입이 심사에 올라가지 않는다.
2. **인앱 구입 생성**: 앱 > 수익 창출 > 인앱 구입 > `+` > 유형 **소모성(Consumable)**.
   - 참조 이름: `팅 70개`
   - 제품 ID: `com.mannabom.ting.70` (되돌릴 수 없으니 규칙을 정해 둔다. 서버 `ting_product.apple_product_id` 와 일치해야 한다)
   - 가격: 가격표에서 티어 선택 (KRW 9,900 등)
   - 현지화(한국어): 표시 이름, 설명
   - 심사 스크린샷: 앱 안에서 이 상품을 사는 화면 캡처 1장
3. **첫 제출**: 인앱 구입은 첫 번째에는 앱 버전 심사와 함께 제출해야 한다. 앱 버전 페이지 하단 "인앱 구입 및 구독" 에서 추가한다. 이후 상품 추가는 단독 제출 가능.
4. **인앱 구입 키 (서버 API 용)**: 사용자 및 액세스 > 통합 > **인앱 구입** > 키 생성. `.p8` 은 한 번만 내려받을 수 있다.
   - `APPLE_APP_STORE_ISSUER_ID`: 같은 화면 상단의 Issuer ID
   - `APPLE_APP_STORE_KEY_ID`: 키 ID
   - `APPLE_APP_STORE_PRIVATE_KEY` 또는 `APPLE_APP_STORE_PRIVATE_KEY_PATH`: `.p8` 내용/경로
5. **앱 식별자**: 앱 > 앱 정보 > 일반 정보의 **Apple ID(숫자)** 를 `APPLE_APP_STORE_APP_APPLE_ID` 에, 번들 ID 를 `APPLE_APP_STORE_BUNDLE_ID` 에 넣는다.
6. **서버 알림 URL**: 앱 > 앱 정보 > **App Store 서버 알림** > 프로덕션/샌드박스 URL 모두 `https://<서버>/api/iap/apple/notifications`, 버전 2 선택. 환불이 발생하면 이 URL 로 REFUND 알림이 오고 서버가 팅을 회수한다.
7. **Apple Root CA**: https://www.apple.com/certificateauthority/ 에서 `Apple Root CA - G3`, `Apple Root CA - G2`, `Apple Inc. Root`, `Apple Computer Inc. Root` 의 `.cer` 파일을 내려받아 서버의 한 디렉터리에 두고 `APPLE_ROOT_CERTIFICATES_PATH` 로 지정한다. JWS 서명 검증에 필요하다.
8. **샌드박스 테스터**: 사용자 및 액세스 > **Sandbox** > 테스터 추가. 기기 설정 > App Store > 샌드박스 계정으로 로그인해서 테스트한다. 심사팀도 샌드박스로 결제하므로 운영 서버는 `APPLE_APP_STORE_SANDBOX_FALLBACK_ENABLED=true` 를 유지해야 한다.

## 5. Google Play Console 신청 절차

1. **판매자 계정**: Play Console > 설정 > 결제 프로필 에서 판매자 계정을 만든다. 이게 없으면 인앱 상품 메뉴가 열리지 않는다.
2. **결제 라이브러리가 포함된 빌드 업로드**: `com.android.vending.BILLING` 권한이 들어간 AAB 를 내부 테스트 트랙에 한 번 올려야 인앱 상품을 만들 수 있다.
3. **인앱 상품 생성**: 수익 창출 > 제품 > **인앱 상품** > 제품 만들기.
   - 제품 ID: `ting_70` (되돌릴 수 없다. 서버 `ting_product.google_product_id` 와 일치)
   - 이름/설명, 가격 설정 후 **활성화**. 활성화 후 기기에 반영되기까지 수 시간 걸릴 수 있다.
4. **API 사용 설정**: GCP 프로젝트 `mannabom-prod` > API 및 서비스 > **Google Play Android Developer API** 사용. Play Console 에서 프로젝트를 따로 연결하는 단계는 없어졌다.
5. **서비스 계정**: `mannabom-prod` > IAM > 서비스 계정 `play-billing` 만들기 > 키(JSON) 생성. Play Console **사용자 및 권한 > 새 사용자 초대**로 서비스 계정 이메일을 초대하고 앱 권한에 다음을 준다.
   - 재무 데이터 보기
   - **주문 및 구독 관리** (구매 조회, consume, 환불 목록에 필요)
   - 권한 부여 후 API 가 실제로 열리기까지 최대 24~36시간 걸릴 수 있다. 그 전에는 401/403 이 난다.
   - 조직 정책 `iam.disableServiceAccountKeyCreation`, `iam.managed.disableServiceAccountKeyCreation` 이 키 생성을 막는다. 키를 새로 만들 때만 프로젝트에서 두 정책을 잠깐 재정의(미적용)하고, 만든 뒤 상속으로 되돌린다. 기존 키는 계속 동작한다.
   - 운영 VM 에는 키를 `/opt/mannabom/google-play/service-account.json` 에 두고 `GOOGLE_PLAY_SERVICE_ACCOUNT_PATH` 로 지정한다(컨테이너에 읽기 전용 연결). `GOOGLE_PLAY_PACKAGE_NAME=com.mannabom.app`.
6. **라이선스 테스터**: 설정 > 라이선스 테스트 에 테스트 계정을 넣으면 실제 결제 없이 테스트할 수 있다. 이 구매는 `purchaseType=0` 으로 오고 서버는 `SANDBOX` 로 기록한다.
7. **환불 처리**: Google 은 환불을 서버로 푸시하지 않는다(RTDN 은 Pub/Sub 설정이 별도로 필요). 서버는 `purchases.voidedpurchases.list` 를 1시간마다 폴링해 환불된 구매의 팅을 회수한다. 운영에서 `TING_PURCHASE_VOIDED_SYNC_ENABLED=true`.

## 6. 환경 변수 요약

```
# Apple
APPLE_APP_STORE_ISSUER_ID=
APPLE_APP_STORE_KEY_ID=
APPLE_APP_STORE_PRIVATE_KEY=            # .p8 내용 (또는 APPLE_APP_STORE_PRIVATE_KEY_PATH)
APPLE_APP_STORE_BUNDLE_ID=
APPLE_APP_STORE_APP_APPLE_ID=           # 숫자 Apple ID, PRODUCTION 검증에 필수
APPLE_APP_STORE_ENVIRONMENT=PRODUCTION  # 로컬/개발은 SANDBOX
APPLE_ROOT_CERTIFICATES_PATH=/etc/mannabom/apple-root-ca

# Google
GOOGLE_PLAY_PACKAGE_NAME=
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON=       # 또는 GOOGLE_PLAY_SERVICE_ACCOUNT_PATH

# 정책
TING_PURCHASE_ALLOW_SANDBOX_CREDIT=true # 심사/테스트 구매에도 팅 지급
TING_PURCHASE_REQUIRE_ACCOUNT_TOKEN=false
TING_PURCHASE_VOIDED_SYNC_ENABLED=true  # 운영만
```

설정이 비어 있어도 서버는 기동된다. 해당 스토어의 첫 검증 요청에서 어떤 설정이 빠졌는지 `IllegalStateException` 메시지로 알려 준다.

## 7. 운영 시 확인할 것

- `payment.status = VERIFYING` 이 오래 남아 있는 건: 스토어 통신 장애 또는 앱이 재시도를 안 한 경우. `failure_reason` 을 본다.
- `store_payment.consume_status = FAILED`: Google consume 재시도 한도(기본 10회) 초과. 서비스 계정 권한을 확인하고 `consume_attempt_count` 를 낮춰 재시도시킨다.
- 환불 회수 부족: `ting_purchase.clawed_back_ting < credited_ting` 인 행. 사용자가 팅을 이미 써버린 경우로, 반복되면 계정 제재 대상이다.
- Apple 알림 테스트: App Store Server API `requestTestNotification` 을 호출하면 `TEST` 알림이 와야 한다.
