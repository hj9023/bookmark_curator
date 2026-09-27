import { useState } from "react";
import BookmarkForm from "./components/BookmarkForm";
import FilterBar from "./components/FilterBar";
import BookmarkList from "./components/BookmarkList";

// 실제 BookmarkResponse 형태와 동일한 더미 데이터
const DUMMY_BOOKMARKS = [
  {
    id: 1,
    url: "https://github.com/facebook/react",
    title: "facebook/react: The library for web and native user interfaces",
    memo: "React 소스코드 및 릴리즈 노트 확인용",
    summary:
      "사용자 인터페이스 구축을 위한 선언적이고 효율적인 자바스크립트 라이브러리입니다. 컴포넌트 기반 설계를 통해 복잡한 웹 UI를 재사용 가능한 독립 단위로 모듈화할 수 있습니다.",
    contentType: "GITHUB",
    tags: ["react", "frontend", "javascript"],
    createdAt: "2026-09-27T08:00:00",
  },
  {
    id: 2,
    url: "https://martinfowler.com/articles/microservices.html",
    title: "Microservices Guide by Martin Fowler",
    memo: "MSA 아키텍처 핵심 개념 정리",
    summary:
      "단일 애플리케이션을 작은 서비스의 조합으로 구축하는 마이크로서비스 아키텍처의 핵심 특성을 설명합니다. 비즈니스 중심의 컴포넌트화와 자동화 배포 체계의 중요성을 다룹니다.",
    contentType: "ARTICLE",
    tags: ["architecture", "microservices", "backend"],
    createdAt: "2026-09-27T08:15:00",
  },
  {
    id: 3,
    url: "https://www.youtube.com/watch?v=spring-boot-4",
    title: "Spring Boot 4.x 완벽 입문 강의 실습",
    memo: "주말에 정독할 강의 영상",
    summary:
      "Spring Boot 최신 버전의 주요 변경점과 REST API 구축 과정을 실습과 함께 설명하는 영상입니다. 자동 설정 및 모던 자바 활용법을 빠르고 직관적으로 배울 수 있습니다.",
    contentType: "VIDEO",
    tags: ["springboot", "java", "backend"],
    createdAt: "2026-09-27T08:30:00",
  },
  {
    id: 4,
    url: "https://tailwindcss.com/docs/installation",
    title: "Tailwind CSS v4 공식 설치 및 설정 가이드",
    memo: "Vite 플러그인 설정 방법 참조",
    summary:
      "Tailwind CSS v4의 새로운 Vite 플러그인 설정 방식과 유틸리티 클래스 사용법을 안내합니다. CSS 파일 하나로 손쉽게 최적화된 모던 웹 스타일링을 구성할 수 있습니다.",
    contentType: "DOCUMENT",
    tags: ["tailwind", "css", "vite"],
    createdAt: "2026-09-27T08:45:00",
  },
];

export default function App() {
  const [bookmarks] = useState(DUMMY_BOOKMARKS);

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 pb-16 font-sans">
      {/* 상단 고정 영역 (URL 입력 + 저장) */}
      <BookmarkForm />

      {/* 본문 영역 (1200px 데스크탑 뷰 기준) */}
      <main className="w-[1200px] mx-auto">
        {/* 필터 영역 (타입 필터 + 검색) */}
        <FilterBar />

        {/* 카드 목록 영역 (3열 그리드) */}
        <BookmarkList bookmarks={bookmarks} />
      </main>
    </div>
  );
}
