import { API_BASE_URL } from "./client";

/**
 * 북마크 목록 조회 (GET /api/bookmarks)
 * @param {Object} [params] - 조회 필터 파라미터
 * @param {string} [params.contentType] - ARTICLE, GITHUB, VIDEO, IMAGE, DOCUMENT, OTHER
 * @param {string} [params.tag] - 태그명
 * @param {string} [params.keyword] - 검색 키워드 (제목, 메모)
 * @returns {Promise<Array>} 북마크 목록
 */
export async function getBookmarks(params = {}) {
  const queryParams = new URLSearchParams();

  if (params.contentType && params.contentType !== "ALL") {
    queryParams.append("contentType", params.contentType);
  }
  if (params.tag && params.tag.trim()) {
    queryParams.append("tag", params.tag.trim());
  }
  if (params.keyword && params.keyword.trim()) {
    queryParams.append("keyword", params.keyword.trim());
  }

  const queryString = queryParams.toString();
  const url = `${API_BASE_URL}/api/bookmarks${queryString ? `?${queryString}` : ""}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message = errorData?.message || `북마크 목록을 불러오지 못했습니다. (HTTP ${response.status})`;
    throw new Error(message);
  }

  return response.json();
}

/**
 * 북마크 생성 (POST /api/bookmarks)
 * @param {Object} bookmarkData
 * @param {string} bookmarkData.url - 저장할 URL
 * @param {string} [bookmarkData.title] - 사용자 지정 제목 (선택)
 * @param {string} [bookmarkData.memo] - 사용자 메모 (선택)
 * @returns {Promise<Object>} 생성된 BookmarkResponse
 */
export async function createBookmark(bookmarkData) {
  const response = await fetch(`${API_BASE_URL}/api/bookmarks`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(bookmarkData),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message = errorData?.message || `북마크 저장에 실패했습니다. (HTTP ${response.status})`;
    throw new Error(message);
  }

  return response.json();
}

/**
 * 북마크 수정 (PATCH /api/bookmarks/{id})
 * @param {number|string} id - 북마크 ID
 * @param {Object} updateData
 * @param {string} [updateData.title] - 제목
 * @param {string} [updateData.memo] - 메모
 * @param {string} [updateData.contentType] - ARTICLE, GITHUB, VIDEO, IMAGE, DOCUMENT, OTHER
 * @param {string[]} [updateData.tags] - 태그 목록
 * @returns {Promise<Object>} 수정된 BookmarkResponse
 */
export async function updateBookmark(id, updateData) {
  const response = await fetch(`${API_BASE_URL}/api/bookmarks/${id}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(updateData),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message = errorData?.message || `북마크 수정에 실패했습니다. (HTTP ${response.status})`;
    throw new Error(message);
  }

  return response.json();
}

/**
 * 북마크 삭제 (DELETE /api/bookmarks/{id})
 * @param {number|string} id - 북마크 ID
 * @returns {Promise<void>}
 */
export async function deleteBookmark(id) {
  const response = await fetch(`${API_BASE_URL}/api/bookmarks/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message = errorData?.message || `북마크 삭제에 실패했습니다. (HTTP ${response.status})`;
    throw new Error(message);
  }
}
