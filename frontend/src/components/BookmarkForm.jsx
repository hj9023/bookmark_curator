import { useState } from "react";
import { createBookmark } from "../api/bookmarkApi";

export default function BookmarkForm({ onBookmarkCreated }) {
  const [url, setUrl] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    const trimmedUrl = url.trim();
    if (!trimmedUrl) {
      setErrorMessage("저장할 URL을 입력해 주세요.");
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMessage(null);

      // POST /api/bookmarks 호출
      const newBookmark = await createBookmark({ url: trimmedUrl });

      // 성공 시: 응답으로 온 BookmarkResponse를 목록 맨 앞에 추가하고 입력창 비우기
      if (onBookmarkCreated) {
        onBookmarkCreated(newBookmark);
      }
      setUrl("");
    } catch (err) {
      console.error("북마크 저장 실패:", err);
      // 실패 시: ErrorResponse.message를 상태로 저장하여 인라인 배너로 표시
      setErrorMessage(err.message || "북마크 저장 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-gray-200 shadow-xs mb-8">
      <div className="w-[1200px] mx-auto py-4">
        {/* 서비스 타이틀 */}
        <div className="flex items-center gap-2 mb-3">
          <span className="text-2xl">🔖</span>
          <h1 className="text-xl font-bold text-gray-900 tracking-tight">북마크 큐레이션</h1>
        </div>

        {/* URL 입력창 + 저장 버튼 */}
        <form onSubmit={handleSubmit} className="flex gap-3">
          <input
            type="url"
            value={url}
            onChange={(e) => {
              setUrl(e.target.value);
              if (errorMessage) setErrorMessage(null);
            }}
            placeholder="저장할 URL을 입력하세요 (예: https://...)"
            disabled={isSubmitting}
            className="flex-1 px-4 py-2.5 bg-gray-50 border border-gray-300 rounded-lg text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all disabled:bg-gray-100 disabled:cursor-not-allowed"
          />
          <button
            type="submit"
            disabled={isSubmitting}
            className={`px-6 py-2.5 bg-blue-600 text-white font-medium text-sm rounded-lg shadow-xs transition-all flex items-center justify-center gap-2 min-w-[100px] ${
              isSubmitting ? "cursor-not-allowed opacity-75" : "hover:bg-blue-700 hover:shadow-sm cursor-pointer"
            }`}
          >
            {isSubmitting ? (
              <>
                <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                <span>저장 중...</span>
              </>
            ) : (
              "저장"
            )}
          </button>
        </form>

        {/* 실패 시 인라인 에러 배너 (빨간 배경) */}
        {errorMessage && (
          <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-bold text-red-500">⚠️</span>
              <span>{errorMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="text-red-400 hover:text-red-600 text-xs font-semibold cursor-pointer px-1 py-0.5"
            >
              ✕
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
