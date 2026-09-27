import { useState } from "react";
import { updateBookmark, deleteBookmark } from "../api/bookmarkApi";

const TYPE_BADGE_STYLES = {
  ARTICLE: "bg-blue-50 text-blue-700 border-blue-200",
  GITHUB: "bg-zinc-100 text-zinc-800 border-zinc-300",
  VIDEO: "bg-red-50 text-red-700 border-red-200",
  IMAGE: "bg-emerald-50 text-emerald-700 border-emerald-200",
  DOCUMENT: "bg-amber-50 text-amber-800 border-amber-200",
  OTHER: "bg-purple-50 text-purple-700 border-purple-200",
};

const CONTENT_TYPES = ["ARTICLE", "GITHUB", "VIDEO", "IMAGE", "DOCUMENT", "OTHER"];

export default function BookmarkCard({ bookmark, onBookmarkUpdated, onBookmarkDeleted }) {
  const { id, url, title, memo, summary, contentType, tags = [] } = bookmark;

  // 카드 상태: 펼침 여부 (보기 모드용) & 수정 모드 여부
  const [isExpanded, setIsExpanded] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // 수정 모드 폼 필드 상태
  const [editTitle, setEditTitle] = useState(title || "");
  const [editMemo, setEditMemo] = useState(memo || "");
  const [editTags, setEditTags] = useState((tags || []).join(", "));
  const [editContentType, setEditContentType] = useState(contentType);

  // 비동기 요청 상태 및 에러
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  // 수정 모드 진입
  const handleStartEdit = (e) => {
    e.stopPropagation();
    setEditTitle(title || "");
    setEditMemo(memo || "");
    setEditTags((tags || []).join(", "));
    setEditContentType(contentType);
    setErrorMessage(null);
    setIsEditing(true);
  };

  // 수정 취소
  const handleCancelEdit = (e) => {
    e.stopPropagation();
    setEditTitle(title || "");
    setEditMemo(memo || "");
    setEditTags((tags || []).join(", "));
    setEditContentType(contentType);
    setErrorMessage(null);
    setIsEditing(false);
  };

  // 수정 저장 (PATCH /api/bookmarks/{id})
  const handleSaveEdit = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    const trimmedTitle = editTitle.trim();
    if (!trimmedTitle) {
      setErrorMessage("제목을 입력해 주세요.");
      return;
    }

    // 쉼표로 구분된 태그 문자열을 배열로 변환
    const parsedTags = editTags
      .split(",")
      .map((t) => t.trim())
      .filter(Boolean);

    try {
      setIsSaving(true);
      setErrorMessage(null);

      const updateData = {
        title: trimmedTitle,
        memo: editMemo.trim(),
        contentType: editContentType,
        tags: parsedTags,
      };

      const updated = await updateBookmark(id, updateData);

      // 성공 시: 부모 상태 갱신 및 보기 모드 복귀
      if (onBookmarkUpdated) {
        onBookmarkUpdated(updated);
      }
      setIsEditing(false);
    } catch (err) {
      console.error("북마크 수정 실패:", err);
      setErrorMessage(err.message || "북마크 수정 중 오류가 발생했습니다.");
    } finally {
      setIsSaving(false);
    }
  };

  // 북마크 삭제 (DELETE /api/bookmarks/{id})
  const handleDelete = async (e) => {
    e.stopPropagation();

    // 1. window.confirm 확인창
    const confirmed = window.confirm("정말 삭제하시겠습니까?");
    if (!confirmed) {
      return;
    }

    try {
      setIsDeleting(true);
      setErrorMessage(null);

      // 2. DELETE /api/bookmarks/{id} 호출
      await deleteBookmark(id);

      // 3. 성공 시: 부모에게 삭제된 id 전달하여 배열에서 제거
      if (onBookmarkDeleted) {
        onBookmarkDeleted(id);
      }
    } catch (err) {
      console.error("북마크 삭제 실패:", err);
      // 4. 실패 시: ErrorResponse.message를 인라인 배너로 표시
      setErrorMessage(err.message || "북마크 삭제 중 오류가 발생했습니다.");
    } finally {
      setIsDeleting(false);
    }
  };

  const toggleExpand = () => {
    if (!isEditing) {
      setIsExpanded((prev) => !prev);
    }
  };

  const badgeStyle = TYPE_BADGE_STYLES[contentType] || "bg-gray-100 text-gray-700 border-gray-200";

  // ========================================================
  // [수정 모드] 렌더링
  // ========================================================
  if (isEditing) {
    return (
      <article
        onClick={(e) => e.stopPropagation()}
        className="bg-white rounded-xl border-2 border-blue-400 p-5 shadow-md flex flex-col justify-between"
      >
        <form onSubmit={handleSaveEdit} className="space-y-3.5">
          <div className="flex items-center justify-between pb-2 border-b border-gray-100">
            <span className="text-xs font-bold text-blue-600">북마크 수정</span>
            <span className="text-xs text-gray-400">ID: #{id}</span>
          </div>

          {/* 인라인 에러 배너 */}
          {errorMessage && (
            <div className="p-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center justify-between">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-red-500">⚠️</span>
                <span>{errorMessage}</span>
              </div>
              <button
                type="button"
                onClick={() => setErrorMessage(null)}
                className="text-red-400 hover:text-red-600 text-xs font-bold px-1"
              >
                ✕
              </button>
            </div>
          )}

          {/* URL (수정 불가 - 텍스트 표시) */}
          <div>
            <label className="block text-xs font-semibold text-gray-500 mb-1">
              원본 URL <span className="text-gray-400 font-normal">(수정 불가)</span>
            </label>
            <p className="text-xs text-gray-700 bg-gray-50 px-3 py-2 rounded-lg border border-gray-200 truncate">
              {url}
            </p>
          </div>

          {/* 제목 입력 */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">제목</label>
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              disabled={isSaving}
              className="w-full px-3 py-1.5 bg-white border border-gray-300 rounded-lg text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="북마크 제목"
            />
          </div>

          {/* 컨텐츠 타입 (select 드롭다운) */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">콘텐츠 타입</label>
            <select
              value={editContentType}
              onChange={(e) => setEditContentType(e.target.value)}
              disabled={isSaving}
              className="w-full px-3 py-1.5 bg-white border border-gray-300 rounded-lg text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
            >
              {CONTENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>

          {/* 요약 (수정 불가 - 텍스트 표시) */}
          <div>
            <label className="block text-xs font-semibold text-gray-500 mb-1">
              AI 요약 <span className="text-gray-400 font-normal">(수정 불가)</span>
            </label>
            <p className="text-xs text-gray-600 bg-gray-50 p-2.5 rounded-lg border border-gray-200 line-clamp-3 leading-relaxed">
              {summary}
            </p>
          </div>

          {/* 태그 입력 (쉼표로 구분) */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">
              태그 <span className="text-gray-400 font-normal">(쉼표로 구분)</span>
            </label>
            <input
              type="text"
              value={editTags}
              onChange={(e) => setEditTags(e.target.value)}
              disabled={isSaving}
              className="w-full px-3 py-1.5 bg-white border border-gray-300 rounded-lg text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="react, frontend, javascript"
            />
          </div>

          {/* 메모 입력 */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">메모</label>
            <input
              type="text"
              value={editMemo}
              onChange={(e) => setEditMemo(e.target.value)}
              disabled={isSaving}
              className="w-full px-3 py-1.5 bg-white border border-gray-300 rounded-lg text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="개인 메모 입력"
            />
          </div>

          {/* 하단 저장/취소 액션 버튼 */}
          <div className="flex items-center justify-end gap-2 pt-3 border-t border-gray-100">
            <button
              type="button"
              onClick={handleCancelEdit}
              disabled={isSaving}
              className="px-3.5 py-1.5 text-xs font-medium text-gray-600 hover:text-gray-900 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="px-4 py-1.5 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-lg shadow-xs transition-all flex items-center justify-center gap-1.5 min-w-[64px] cursor-pointer disabled:opacity-75 disabled:cursor-not-allowed"
            >
              {isSaving ? (
                <>
                  <span className="inline-block w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  <span>저장 중...</span>
                </>
              ) : (
                "저장"
              )}
            </button>
          </div>
        </form>
      </article>
    );
  }

  // ========================================================
  // [보기 모드] 렌더링
  // ========================================================
  return (
    <article
      onClick={toggleExpand}
      className="bg-white rounded-xl border border-gray-200 p-5 shadow-xs flex flex-col justify-between hover:shadow-md hover:border-gray-300 transition-all cursor-pointer select-none"
    >
      <div>
        {/* 삭제 에러 등 인라인 에러 배너 (보기 모드) */}
        {errorMessage && (
          <div className="mb-3 p-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-red-500">⚠️</span>
              <span>{errorMessage}</span>
            </div>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setErrorMessage(null);
              }}
              className="text-red-400 hover:text-red-600 text-xs font-bold px-1 cursor-pointer"
            >
              ✕
            </button>
          </div>
        )}

        {/* 상단: 타입 뱃지 및 펼침 힌트 */}
        <div className="flex items-center justify-between mb-3">
          <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${badgeStyle}`}>{contentType}</span>
          <span className="text-xs text-gray-400 hover:text-blue-600 transition-colors">
            {isExpanded ? "접기 ▲" : "전체보기 ▼"}
          </span>
        </div>

        {/* 제목 (접힘 시 1줄 제한, 전체보기 시 전체 표시) */}
        <h3
          className={`text-base font-bold text-gray-900 mb-2 break-words leading-snug ${
            isExpanded ? "" : "line-clamp-1"
          }`}
        >
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="hover:text-blue-600 transition-colors cursor-pointer"
          >
            {title}
          </a>
        </h3>

        {/* 요약 (클릭 시 3줄 제한 <-> 전체 펼침 토글) */}
        <div className="mb-4">
          <p
            className={`text-sm text-gray-600 leading-relaxed transition-all ${
              isExpanded ? "whitespace-pre-line" : "line-clamp-3"
            }`}
          >
            {summary}
          </p>

          {/* 펼쳐졌을 때 메모가 있으면 함께 표시 */}
          {isExpanded && memo && (
            <div className="mt-3 p-3 bg-amber-50/80 border border-amber-200/80 rounded-lg text-xs text-amber-900 leading-relaxed">
              <span className="font-semibold block mb-1">📝 메모:</span>
              {memo}
            </div>
          )}
        </div>
      </div>

      <div>
        {/* 태그 목록 */}
        <div className="flex flex-wrap gap-1.5 mb-4">
          {tags.map((tag, idx) => (
            <span
              key={`${id}-tag-${idx}`}
              onClick={(e) => e.stopPropagation()}
              className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-md hover:bg-gray-200 transition-colors"
            >
              #{tag}
            </span>
          ))}
        </div>

        {/* 하단 액션 버튼 */}
        <div className="flex items-center justify-end gap-2 pt-3 border-t border-gray-100">
          <button
            type="button"
            onClick={handleStartEdit}
            disabled={isDeleting}
            className="text-xs font-medium text-gray-600 hover:text-blue-600 px-3 py-1.5 rounded-md border border-gray-200 hover:border-blue-300 hover:bg-blue-50 transition-colors cursor-pointer disabled:opacity-50"
          >
            수정
          </button>
          <button
            type="button"
            onClick={handleDelete}
            disabled={isDeleting}
            className={`text-xs font-medium px-3 py-1.5 rounded-md border transition-colors ${
              isDeleting
                ? "text-gray-400 border-gray-200 bg-gray-50 cursor-not-allowed"
                : "text-gray-600 hover:text-red-600 border-gray-200 hover:border-red-300 hover:bg-red-50 cursor-pointer"
            }`}
          >
            {isDeleting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </div>
    </article>
  );
}
