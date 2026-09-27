import BookmarkCard from "./BookmarkCard";

export default function BookmarkList({ bookmarks = [] }) {
  if (bookmarks.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500">
        등록된 북마크가 없습니다. 상단에서 URL을 입력하여 저장해보세요.
      </div>
    );
  }

  return (
    <section className="grid grid-cols-3 gap-6">
      {bookmarks.map((bookmark) => (
        <BookmarkCard key={bookmark.id} bookmark={bookmark} />
      ))}
    </section>
  );
}
