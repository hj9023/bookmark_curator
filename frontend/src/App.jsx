import { useState, useEffect } from "react";
import BookmarkForm from "./components/BookmarkForm";
import FilterBar from "./components/FilterBar";
import BookmarkList from "./components/BookmarkList";
import { getBookmarks } from "./api/bookmarkApi";

export default function App() {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let ignore = false;

    async function loadBookmarks() {
      try {
        setLoading(true);
        setError(null);
        const data = await getBookmarks();
        if (!ignore) {
          setBookmarks(data);
        }
      } catch (err) {
        if (!ignore) {
          console.error("북마크 목록 불러오기 실패:", err);
          setError(err.message || "북마크 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadBookmarks();

    return () => {
      ignore = true;
    };
  }, []);

  const handleBookmarkCreated = (newBookmark) => {
    // 성공 시: 응답으로 온 BookmarkResponse를 그대로 목록 맨 앞에 추가
    setBookmarks((prev) => [newBookmark, ...prev]);
  };

  const handleBookmarkUpdated = (updatedBookmark) => {
    // 성공 시: 전체 목록을 다시 불러오지 않고, 부모 배열 state에서 해당 id 항목만 교체
    setBookmarks((prev) => prev.map((item) => (item.id === updatedBookmark.id ? updatedBookmark : item)));
  };

  const handleBookmarkDeleted = (deletedId) => {
    // 성공 시: 부모에게 삭제된 id를 알려서, 목록 배열에서 해당 항목만 제거
    setBookmarks((prev) => prev.filter((item) => item.id !== deletedId));
  };

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 pb-16 font-sans">
      {/* 상단 고정 영역 (URL 입력 + 저장) */}
      <BookmarkForm onBookmarkCreated={handleBookmarkCreated} />

      {/* 본문 영역 (1200px 데스크탑 뷰 기준) */}
      <main className="w-[1200px] mx-auto">
        {/* 필터 영역 (타입 필터 + 검색) */}
        <FilterBar />

        {/* 카드 목록 영역 (3열 그리드) */}
        <BookmarkList
          bookmarks={bookmarks}
          loading={loading}
          error={error}
          onBookmarkUpdated={handleBookmarkUpdated}
          onBookmarkDeleted={handleBookmarkDeleted}
        />
      </main>
    </div>
  );
}
