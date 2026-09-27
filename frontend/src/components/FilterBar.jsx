import { useState } from "react";

const FILTER_TYPES = [
  { label: "전체", value: "ALL" },
  { label: "ARTICLE", value: "ARTICLE" },
  { label: "GITHUB", value: "GITHUB" },
  { label: "VIDEO", value: "VIDEO" },
  { label: "IMAGE", value: "IMAGE" },
  { label: "DOCUMENT", value: "DOCUMENT" },
  { label: "OTHER", value: "OTHER" },
];

export default function FilterBar() {
  const [selectedType, setSelectedType] = useState("ALL");
  const [keyword, setKeyword] = useState("");

  const handleTypeClick = (typeValue) => {
    setSelectedType(typeValue);
    console.log("타입 필터 클릭:", typeValue);
  };

  const handleSearchChange = (e) => {
    setKeyword(e.target.value);
    console.log("검색어 입력:", e.target.value);
  };

  return (
    <section className="bg-white rounded-xl border border-gray-200 p-5 mb-6 shadow-xs">
      {/* 타입 필터 7개 버튼 */}
      <div className="flex items-center gap-2 mb-4">
        {FILTER_TYPES.map((type) => {
          const isActive = selectedType === type.value;
          return (
            <button
              key={type.value}
              type="button"
              onClick={() => handleTypeClick(type.value)}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                isActive ? "bg-gray-900 text-white shadow-xs" : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {type.label}
            </button>
          );
        })}
      </div>

      {/* 검색창 */}
      <div>
        <input
          type="text"
          value={keyword}
          onChange={handleSearchChange}
          placeholder="검색창 (제목, 메모 등 키워드 검색)"
          className="w-full px-4 py-2.5 bg-gray-50 border border-gray-300 rounded-lg text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all"
        />
      </div>
    </section>
  );
}
