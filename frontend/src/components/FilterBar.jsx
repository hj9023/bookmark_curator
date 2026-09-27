const FILTER_TYPES = ["전체", "ARTICLE", "GITHUB", "VIDEO", "IMAGE", "DOCUMENT", "OTHER"];

export default function FilterBar({ selectedType = "전체", onSelectType, keyword = "", onKeywordChange }) {
  return (
    <section className="bg-white rounded-xl border border-gray-200 p-5 mb-6 shadow-xs">
      {/* 타입 필터 7개 버튼 */}
      <div className="flex items-center gap-2 mb-4">
        {FILTER_TYPES.map((type) => {
          const isActive = selectedType === type;
          return (
            <button
              key={type}
              type="button"
              onClick={() => onSelectType && onSelectType(type)}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                isActive ? "bg-gray-900 text-white shadow-xs" : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {type}
            </button>
          );
        })}
      </div>

      {/* 실시간 검색창 (onChange 즉시 반영) */}
      <div>
        <input
          type="text"
          value={keyword}
          onChange={(e) => onKeywordChange && onKeywordChange(e.target.value)}
          placeholder="검색창 (제목, 요약 등 실시간 검색)"
          className="w-full px-4 py-2.5 bg-gray-50 border border-gray-300 rounded-lg text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all"
        />
      </div>
    </section>
  );
}
