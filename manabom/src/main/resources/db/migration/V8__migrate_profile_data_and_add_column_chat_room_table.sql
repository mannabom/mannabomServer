-- 1. [데이터 이관] 기존 문자열(university) -> 새로운 ID(university_id) 매핑
-- 이름이 정확히 일치하는 경우에만 업데이트
UPDATE public.profile p
SET university_id = u.university_id
    FROM public.university u
WHERE p.university = u.name
  AND p.university_id IS NULL; -- 이미 값이 있는 건 건드리지 않음

-- 2. [데이터 이관] 기존 문자열(region) -> 새로운 ID(region_id) 매핑
-- 시도/시군구 이름이 모두 일치하는 경우 업데이트
UPDATE public.profile p
SET region_id = r.sigungu_code
    FROM public.regions r
WHERE p.region_sido = r.sido_name
  AND p.region_sigungu = r.sigungu_name
  AND p.region_id IS NULL;

-- 3. [컬럼 삭제] 데이터 이관이 끝났으므로 기존 컬럼 삭제
ALTER TABLE public.profile DROP COLUMN IF EXISTS university;
ALTER TABLE public.profile DROP COLUMN IF EXISTS region_sido;
ALTER TABLE public.profile DROP COLUMN IF EXISTS region_sigungu;


-- V5__add_recommend_history_to_chat_rooms.sql



-- 1. 새로운 연관관계 컬럼 추가
ALTER TABLE public.chat_rooms ADD COLUMN IF NOT EXISTS profile_recommend_history_id int8;
ALTER TABLE public.chat_rooms ADD COLUMN IF NOT EXISTS love_view_recommend_history_id int8;

-- 2. 1:1 관계를 위한 UNIQUE 제약 조건 추가
ALTER TABLE public.chat_rooms ADD CONSTRAINT uk_chat_profile_history UNIQUE (profile_recommend_history_id);
ALTER TABLE public.chat_rooms ADD CONSTRAINT uk_chat_loveview_history UNIQUE (love_view_recommend_history_id);

-- 3. 외래키(FK) 설정
-- 참조하는 테이블 이름(profile_recommend_history 등)이 실제 생성된 이름과 맞는지 확인하세요!
ALTER TABLE public.chat_rooms
    ADD CONSTRAINT fk_chat_profile_history FOREIGN KEY (profile_recommend_history_id)
        REFERENCES public.profile_recommend_history(id);

ALTER TABLE public.chat_rooms
    ADD CONSTRAINT fk_chat_loveview_history FOREIGN KEY (love_view_recommend_history_id)
        REFERENCES public.love_view_recommend_history(id);