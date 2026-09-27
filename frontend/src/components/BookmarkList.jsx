import BookmarkCard from "./BookmarkCard";

export default function BookmarkList({ bookmarks = [], loading = false, error = null }) {
  // 1. 로딩 중 표시
  if (loading) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 shadow-xs">
        <div className="inline-block animate-spin rounded-full h-6 w-6 border-2 border-blue-600 border-t-transparent mb-2"></div>
        <p className="text-sm font-medium text-gray-600">불러오는 중...</p>
      </div>
    );
  }

  // 에러 발생 시 안내 표시
  if (error) {
    return (
      <div className="bg-white rounded-xl border border-red-200 p-12 text-center text-red-500 shadow-xs">
        <p className="text-sm font-medium mb-1">{error}</p>
        <p className="text-xs text-gray-400">백엔드 서버(http://localhost:8080) 기동 상태를 확인해 주세요.</p>
      </div>
    );
  }

  // 2. 목록이 비어있을 때 표시
  if (bookmarks.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 shadow-xs">
        저장된 북마크가 없습니다
      </div>
    );
  }

  // 3. 실제 북마크 목록 카드 그리드
  return (
    <section className="grid grid-cols-3 gap-6">
      {bookmarks.map((bookmark) => (
        <BookmarkCard key={bookmark.id} bookmark={bookmark} />
      ))}
    </section>
  );
}
