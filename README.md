# AI 북마크 큐레이션 서비스

URL을 저장하면 LLM이 자동으로 요약과 태그를 생성해주는 개인용 북마크 매니저.
Spring Boot(+React) 기반, 2~3일 내 구현을 목표로 한 바이브 코딩 프로젝트.

## 목표

- LLM과의 협업(바이브 코딩)으로 짧은 기간 안에 동작하는 풀스택 서비스를 구현
- 사람이 구조(엔티티/API 계약/패키지 구조)를 먼저 정하고, LLM이 세부 구현을 담당하는 방식으로 진행
- 인증/멀티유저 기능은 스코프에서 제외 — 핵심 기능(콘텐츠 타입 판별 → LLM 요약/태깅 파이프라인)에 시간을 집중 배분

## 스코프 (CRUD)

| 기능   | 설명                                                                           |
| ------ | ------------------------------------------------------------------------------ |
| Create | URL 입력 → 콘텐츠 타입 판별 → 본문/메타데이터 수집 → LLM 요약·태그 생성 → 저장 |
| Read   | 목록 조회, `contentType`/태그 필터, 키워드 검색(제목/메모)                     |
| Update | 제목/메모/태그/콘텐츠타입 인라인 수정 (LLM 결과 및 타입 보정용)                |
| Delete | 북마크 삭제                                                                    |

## 엔티티 설계

### Bookmark

| 필드        | 타입                                                 | 설명                                       |
| ----------- | ---------------------------------------------------- | ------------------------------------------ |
| id          | Long (PK)                                            |                                            |
| url         | String, not null                                     | 원본 URL                                   |
| title       | String, not null                                     | 사용자 입력 우선, 없으면 LLM 생성값        |
| memo        | String, nullable                                     | 사용자 메모                                |
| summary     | String(TEXT)                                         | LLM 생성 3줄 요약                          |
| contentType | Enum(ARTICLE, GITHUB, VIDEO, IMAGE, DOCUMENT, OTHER) | 서버가 URL로 자동 판별 (수정 시 변경 가능) |
| createdAt   | LocalDateTime                                        | 자동 기록                                  |
| updatedAt   | LocalDateTime                                        | 인라인 수정 시 갱신                        |

### Tag

| 필드 | 타입                     | 설명                        |
| ---- | ------------------------ | --------------------------- |
| id   | Long (PK)                |                             |
| name | String, unique, not null | 정규화(소문자/trim) 후 저장 |

### 관계

- `Bookmark` : `Tag` = 다대다 (`@ManyToMany` + 조인 테이블 `bookmark_tag`)
- Bookmark 삭제 시 조인 테이블 레코드는 cascade 삭제, Tag 자체는 유지(다른 북마크가 재사용 가능)
- 고아 태그 정리 로직은 스코프 밖

## API 명세

| 메서드 | 경로                | 설명                            | 요청                                     | 응답                     |
| ------ | ------------------- | ------------------------------- | ---------------------------------------- | ------------------------ |
| POST   | /api/bookmarks      | URL 저장 + LLM 요약/태그 생성   | `{ url, title?, memo? }`                 | `BookmarkResponse`       |
| GET    | /api/bookmarks      | 목록 조회                       | 쿼리파람: contentType, tag, keyword      | `List<BookmarkResponse>` |
| PATCH  | /api/bookmarks/{id} | 제목/메모/태그/타입 인라인 수정 | `{ title?, memo?, tags?, contentType? }` | `BookmarkResponse`       |
| DELETE | /api/bookmarks/{id} | 삭제                            | -                                        | 204                      |

### BookmarkResponse 예시

```json
{
  "id": 1,
  "url": "https://example.com/article",
  "title": "예시 아티클 제목",
  "memo": "나중에 다시 읽어볼 것",
  "summary": "이 글은 ... 3줄 요약 내용",
  "contentType": "ARTICLE",
  "tags": ["ai", "개발", "생산성"],
  "createdAt": "2026-09-26T10:00:00"
}
```

## 콘텐츠 타입 판별 & 요약 파이프라인

1. **타입 판별 (코드, LLM 미사용)**
   - `youtube.com` / `youtu.be` → VIDEO
   - `github.com` → GITHUB
   - 이미지 확장자(`.png`, `.jpg` 등) → IMAGE
   - 그 외 URL fetch 성공 → ARTICLE
   - fetch 실패 → OTHER
2. **콘텐츠 수집 (타입별로 다르게)**
   - ARTICLE / GITHUB: 본문 또는 README 텍스트 fetch
   - VIDEO: 제목 + 설명(oEmbed 또는 og:description)
   - IMAGE / 기타: 텍스트 추출 어려우면 메타데이터(제목, 파일명)만 사용, 요약 실패 시 "요약 불가 — 메모를 직접 입력해주세요" 처리
3. **LLM 호출은 프롬프트 1개, 변수만 주입**
   - `{contentType}`, `{수집된 텍스트}`를 템플릿에 삽입
   - 출력: 3줄 요약 + 태그 3~5개
   - 기존 태그와 대소문자/공백 정규화 후 매칭, 없으면 신규 생성

## 네이밍 컨벤션

- **클래스명**: PascalCase — `Bookmark`, `BookmarkService`, `BookmarkController`
- **DTO 접미사**: 요청은 `~Request`, 응답은 `~Response` (`BookmarkRequest`, `BookmarkResponse`). Entity를 DTO 접미사 없이 그대로 반환하지 않음
- **메서드명**: `find~`(단건 조회) / `findAll~`(목록 조회) / `save~`(생성·수정) / `delete~`(삭제)로 통일. `get`/`fetch` 등 유사어 혼용 금지
- **REST 경로**: 복수형 명사, kebab-case — `/api/bookmarks`, `/api/bookmarks/{id}`
- **DB 컬럼/테이블**: snake_case — `content_type`, `created_at`
- **React**: 컴포넌트 파일명은 PascalCase(`BookmarkList.jsx`), 커스텀 훅은 `use~` 접두사(`useBookmarks.js`)

## 패키지 구조

```
src/main/java/.../bookmark
 ├── controller/BookmarkController.java
 ├── service/
 │    ├── BookmarkService.java
 │    ├── ContentFetchService.java   // URL fetch + contentType 판별
 │    └── SummaryService.java        // LLM 호출 (요약/태그 생성)
 ├── repository/BookmarkRepository.java, TagRepository.java
 ├── domain/Bookmark.java, Tag.java, ContentType.java(enum)
 └── dto/BookmarkRequest.java, BookmarkResponse.java
```

프론트엔드(React): `components/`, `pages/`, `api/` 기본 구조.

## 인증/멀티유저 관련

- 이번 버전에는 회원가입/로그인 없음. 단일 사용자 개인용 툴로 가정
- 이유: 짧은 개발 기간 안에 핵심 기능(콘텐츠 판별 → LLM 파이프라인)에 시간을 집중하기 위함
- 향후 확장 시: `Bookmark`에 `userId` 컬럼 추가 + Spring Security/JWT 도입으로 멀티유저 전환 가능

## Configuration

- application.yaml 사용 (properties 아님) — 계층 구조가 있어서 datasource/jpa 설정 읽기 편함
- spring.jpa.hibernate.ddl-auto: update — 엔티티 수정할 때마다 스키마 자동 반영
- spring.h2.console.enabled: true — 브라우저에서 /h2-console로 DB 상태 바로 확인 가능, 디버깅 속도에 큰 도움
- spring.datasource.url: jdbc:h2:mem:bookmarkdb — 인메모리, 재시작하면 데이터 날아가는 건 감안(로컬 개발 단계라 문제없음)
- CORS 설정 미리 추가: React가 다른 포트(3000 등)에서 호출하니 WebMvcConfigurer로 /api/\*\*에 대해 localhost:3000 허용 — 나중에 프론트 연결할 때 이거 안 해두면 원인 모를 에러로 시간 날림
- 로그 레벨은 org.hibernate.SQL: debug 정도 켜두면 JPA가 실제로 어떤 쿼리 날리는지 보여서 AI가 짠 쿼리 검증하기 편함

## TODO

- 엔티티 설계, Spring Boot REST API(CRUD), DB(H2/MySQL) 구성 |
- React 카드형 목록 UI, 태그/contentType 필터, 검색, API 연동 |
- URL fetch + LLM 요약/태그 생성 연동, 예외처리 |
