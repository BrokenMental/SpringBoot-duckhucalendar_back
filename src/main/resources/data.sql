-- 2025년 한국 공휴일/국경일 데이터 초기화
-- 기존 데이터가 없는 경우에만 실행

-- 신정 (공휴일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '신정', '2025-01-01', 'KR', 'PUBLIC', '새해 첫날', true, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '신정' AND holiday_date = '2025-01-01');

-- 삼일절 (국경일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '삼일절', '2025-03-01', 'KR', 'NATIONAL', '3·1 독립운동 기념일', true, '#4285F4', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '삼일절' AND holiday_date = '2025-03-01');

-- 어린이날 (공휴일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '어린이날', '2025-05-05', 'KR', 'PUBLIC', '어린이날', true, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '어린이날' AND holiday_date = '2025-05-05');

-- 석가탄신일 (공휴일) - 2025년 5월 5일
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '석가탄신일', '2025-05-05', 'KR', 'PUBLIC', '부처님오신날', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '석가탄신일' AND holiday_date = '2025-05-05');

-- 현충일 (기념일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '현충일', '2025-06-06', 'KR', 'MEMORIAL', '호국영령을 추모하는 날', true, '#9C27B0', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '현충일' AND holiday_date = '2025-06-06');

-- 광복절 (국경일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '광복절', '2025-08-15', 'KR', 'NATIONAL', '일제강점기 해방 기념일', true, '#4285F4', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '광복절' AND holiday_date = '2025-08-15');

-- 추석연휴 (공휴일) - 2025년 10월 5일~7일
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '추석연휴', '2025-10-05', 'KR', 'PUBLIC', '추석 연휴 첫째 날', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '추석연휴' AND holiday_date = '2025-10-05');

INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '추석', '2025-10-06', 'KR', 'PUBLIC', '추석 당일', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '추석' AND holiday_date = '2025-10-06');

INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '추석연휴', '2025-10-07', 'KR', 'PUBLIC', '추석 연휴 마지막 날', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '추석연휴' AND holiday_date = '2025-10-07');

-- 개천절 (국경일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '개천절', '2025-10-03', 'KR', 'NATIONAL', '단군왕검이 고조선을 건국한 날', true, '#4285F4', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '개천절' AND holiday_date = '2025-10-03');

-- 한글날 (국경일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '한글날', '2025-10-09', 'KR', 'NATIONAL', '한글 창제를 기념하는 날', true, '#4285F4', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '한글날' AND holiday_date = '2025-10-09');

-- 크리스마스 (공휴일)
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '크리스마스', '2025-12-25', 'KR', 'PUBLIC', '예수 그리스도의 탄생을 기념하는 날', true, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '크리스마스' AND holiday_date = '2025-12-25');

-- 기타 기념일들
-- 설날연휴 (2025년 1월 28일~30일) - 음력이므로 연도별로 다름
INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '설날연휴', '2025-01-28', 'KR', 'PUBLIC', '설날 연휴 첫째 날', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '설날연휴' AND holiday_date = '2025-01-28');

INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '설날', '2025-01-29', 'KR', 'PUBLIC', '설날 당일', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '설날' AND holiday_date = '2025-01-29');

INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
SELECT '설날연휴', '2025-01-30', 'KR', 'PUBLIC', '설날 연휴 마지막 날', false, '#FF6B6B', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '설날연휴' AND holiday_date = '2025-01-30');

-- 대체공휴일 예시 (필요시 추가)
-- 만약 어린이날이 일요일과 겹친다면 대체공휴일이 생김
-- INSERT INTO holidays (name, holiday_date, country_code, holiday_type, description, is_recurring, color, created_at, updated_at)
-- SELECT '어린이날 대체공휴일', '2025-05-06', 'KR', 'SUBSTITUTE', '어린이날 대체공휴일', false, '#FF9800', NOW(), NOW()
-- WHERE NOT EXISTS (SELECT 1 FROM holidays WHERE name = '어린이날 대체공휴일' AND holiday_date = '2025-05-06');
