package com.finger.handoff.domain.curation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class CurationInitController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/init-curations")
    @Transactional
    public ResponseEntity<String> initCurations() {
        // ★★★ [해결 핵심 1줄 추가!] 기존에 꼬인 테이블이나 중복 데이터가 있으면 싹 지우고 새로 시작! ★★★
        jdbcTemplate.execute("DROP TABLE IF EXISTS presentation_contents;");
        log.info("기존 presentation_contents 테이블 초기화(DROP) 완료");

        // 1. 테이블 생성 DDL
        String createTableSql = """
            CREATE TABLE presentation_contents (
                id INT PRIMARY KEY,
                category VARCHAR(50) NOT NULL,
                d_day_type VARCHAR(50) NOT NULL,
                theme VARCHAR(255) NOT NULL,
                order_num INT NOT NULL,
                media_type VARCHAR(20) NOT NULL,
                content_title VARCHAR(255) NOT NULL,
                author_channel VARCHAR(100) NOT NULL,
                url TEXT NOT NULL,
                thumbnail_url TEXT NOT NULL
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """;

        jdbcTemplate.execute(createTableSql);
        log.info("presentation_contents 테이블 생성 완료");

        // 2. 데이터 삽입 DML (이전과 동일한 1번~96번 데이터)
        String insertSql = """
            INSERT INTO presentation_contents 
            (id, category, d_day_type, theme, order_num, media_type, content_title, author_channel, url, thumbnail_url) 
            VALUES
            (1,'EDUCATION','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',1,'영상','학부생을 위한 자료 조사 한 방에 정리하기!','DBpia','https://www.youtube.com/watch?v=YRxDWjfCNk0','https://img.youtube.com/vi/YRxDWjfCNk0/hqdefault.jpg'),
            (2,'EDUCATION','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',2,'영상','말하기의 핵심은 바로 ''키워드'' 입니다.','김홍국TV','https://www.youtube.com/watch?v=24jkZg_dw5w','https://img.youtube.com/vi/-fLRKwy6SRA/hqdefault.jpg'),
            (3,'EDUCATION','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',3,'영상','온라인 학술대회 구경하기 | 구두발표하는 법','슬기로운 연구생활','https://www.youtube.com/watch?v=pFQK0uro_IU','https://img.youtube.com/vi/pFQK0uro_IU/hqdefault.jpg'),
            (4,'EDUCATION','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',4,'아티클','발표 스피치, 어떻게 하면 잘 할 수 있을까요?','교육부 공식 블로그','https://if-blog.tistory.com/11177','https://blog.kakaocdn.net/dna/bf2ULN/btqJOz6sOnf/AAAAAAAAAAAAAAAAAAAAAGX6OE2jt0ZYNKWyVfsxBu9FSFhvfQBAHqU8UVTMrXet/img.png?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=w2yDrlTlWsLCPgovnGuR5VTU%2FVY%3D'),
            (5,'EDUCATION','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',5,'영상','말이 정돈되게 나오지 않는다면, ''이 구조''만 기억하세요! | 발표, 보고, 회의 모두 먹히는 말하기 공식','이교수의 인터랙션 _ 커뮤니케이션 코치','https://youtu.be/CjgefsNYBOI?si=OAmMjzTnTStDAsv1','https://img.youtube.com/vi/CjgefsNYBOI/hqdefault.jpg'),
            (6,'EDUCATION','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',6,'영상','문서와 프레젠테이션의 5단계','파워포인트 블루스','https://www.youtube.com/watch?v=OKOjWrv9fBQ','https://img.youtube.com/vi/OKOjWrv9fBQ/hqdefault.jpg'),
            (7,'EDUCATION','D_6_TO_3','발음과 속도를 점검해보세요',1,'영상','발음이 좋아지는 10가지 문장 / 발음교정 / 발음훈련','MODA TV','https://youtu.be/oR2crUMux0k?si=Tx0Nc8Gj1cddyAfZ','https://img.youtube.com/vi/oR2crUMux0k/hqdefault.jpg'),
            (8,'EDUCATION','D_6_TO_3','발음과 속도를 점검해보세요',2,'영상','잘 들리게 말하는 법, 3가지','민지적 시점','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (9,'EDUCATION','D_6_TO_3','발음과 속도를 점검해보세요',3,'영상','[프레젠테이션 시크릿] 발표 할 때 시간 관리하는 방법','콘텐츠위드','https://youtu.be/RjD5xT7ftME?si=sVzV7VhHyhMqdZip','https://img.youtube.com/vi/RjD5xT7ftME/hqdefault.jpg'),
            (10,'EDUCATION','D_6_TO_3','발음과 속도를 점검해보세요',4,'아티클','발표 스피치, 어떻게 하면 잘 할 수 있을까요?','교육부 공식 블로그','https://if-blog.tistory.com/11177','https://blog.kakaocdn.net/dna/bf2ULN/btqJOz6sOnf/AAAAAAAAAAAAAAAAAAAAAGX6OE2jt0ZYNKWyVfsxBu9FSFhvfQBAHqU8UVTMrXet/img.png?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=w2yDrlTlWsLCPgovnGuR5VTU%2FVY%3D'),
            (11,'EDUCATION','D_6_TO_3','발음과 속도를 점검해보세요',5,'영상','발표자의 위치와 발표시간 관리 기술, 플랫폼스킬','구은화 TV','https://www.youtube.com/watch?v=LGyOqO53mmk','https://img.youtube.com/vi/LGyOqO53mmk/hqdefault.jpg'),
            (12,'EDUCATION','D_6_TO_3','발음과 속도를 점검해보세요',6,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','특수교육학 대학원생 노트','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (13,'EDUCATION','D_2_TO_1','실전처럼 말해보세요',1,'영상','이것만 알면 여러 사람 앞에서도 말 잘할 수 있습니다 | 말하기, 발표, 데일 카네기','책식주의','https://youtu.be/oPoE8dfTybo?si=IMOyHLnFFQjechR0','https://img.youtube.com/vi/oPoE8dfTybo/hqdefault.jpg'),
            (14,'EDUCATION','D_2_TO_1','실전처럼 말해보세요',2,'영상','미국 학회 대학원 수업 프레젠테이션 준비법','교수엄마 Professor Mommy','https://www.youtube.com/watch?v=24jkZg_dw5w','https://img.youtube.com/vi/24jkZg_dw5w/hqdefault.jpg'),
            (15,'EDUCATION','D_2_TO_1','실전처럼 말해보세요',3,'아티클','MIT 강의 정리 | 발표 잘하는 법 & 프레젠테이션 스킬','Tilnote','https://tilnote.io/pages/69cea336a5dad016ee58f602','https://images.tilnote.io/pages/769f8998-605f-45dd-b706-71dece09b43d.webp'),
            (16,'EDUCATION','D_2_TO_1','실전처럼 말해보세요',4,'영상','[24년차 교수] 말 잘하는 방법 | 교수님은 말하기를 어떻게 연습할까?','김교수의 세 가지','https://youtu.be/qn02_22n8gA?si=PGT9MuikBQNzCO0_','https://img.youtube.com/vi/qn02_22n8gA/hqdefault.jpg'),
            (17,'EDUCATION','D_2_TO_1','실전처럼 말해보세요',5,'영상','발표자의 위치와 발표시간 관리 기술, 플랫폼스킬','구은화 TV','https://www.youtube.com/watch?v=LGyOqO53mmk','https://img.youtube.com/vi/LGyOqO53mmk/hqdefault.jpg'),
            (18,'EDUCATION','D_2_TO_1','실전처럼 말해보세요',6,'영상','[프레젠테이션 시크릿] 발표 할 때 시간 관리하는 방법','콘텐츠위드','https://youtu.be/RjD5xT7ftME?si=sVzV7VhHyhMqdZip','https://img.youtube.com/vi/RjD5xT7ftME/hqdefault.jpg'),
            (19,'EDUCATION','D_DAY','발표 전 긴장을 완화해보세요',1,'영상','대중 앞에서 말 잘하는 법','스터디언','https://www.youtube.com/watch?v=nf0pfzoqbeA','https://img.youtube.com/vi/nf0pfzoqbeA/hqdefault.jpg'),
            (20,'EDUCATION','D_DAY','발표 전 긴장을 완화해보세요',2,'영상','발음이 좋아지는 10가지 문장 / 발음교정 / 발음훈련','MODA TV','https://youtu.be/oR2crUMux0k?si=Tx0Nc8Gj1cddyAfZ','https://img.youtube.com/vi/oR2crUMux0k/hqdefault.jpg'),
            (21,'EDUCATION','D_DAY','발표 전 긴장을 완화해보세요',3,'영상','잘 들리게 말하는 법, 3가지','민지적 시점','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (22,'EDUCATION','D_DAY','발표 전 긴장을 완화해보세요',4,'아티클','발표 스피치, 어떻게 하면 잘 할 수 있을까요?','교육부 공식 블로그','https://if-blog.tistory.com/11177','https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdna%2FdFmxP5%2FbtqJINqV6D1%2FAAAAAAAAAAAAAAAAAAAAAMaknE2mBJlRolRZyHAUqxe4wg7nEXjlabSKm9pmJjJv%2Fimg.jpg%3Fcredential%3DyqXZFxpELC7KVnFOS48ylbz2pIh7yKj8%26expires%3D1782831599%26allow_ip%3D%26allow_referer%3D%26signature%3DXjizdijbo5X0vrqcAV%252BBJaQBUCA%253D'),
            (23,'EDUCATION','D_DAY','발표 전 긴장을 완화해보세요',5,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','구은화 TV','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (24,'EDUCATION','D_DAY','발표 전 긴장을 완화해보세요',6,'영상','좋은 스피치를 만드는 방법','세바시 강연','https://www.youtube.com/watch?v=QCHuEcZ9B7Y','https://img.youtube.com/vi/QCHuEcZ9B7Y/hqdefault.jpg'),
            (25,'WORK','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',1,'영상','프레젠테이션 발표자료 잘 만드는 방법 – 제안발표, IR피칭, 보고서 첫 장','구은화 TV','https://www.youtube.com/watch?v=9OjNpMzMjgI','https://img.youtube.com/vi/9OjNpMzMjgI/hqdefault.jpg'),
            (26,'WORK','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',2,'영상','말이 정돈되게 나오지 않는다면, ''이 구조''만 기억하세요! | 발표, 보고, 회의 모두 먹히는 말하기 공식','이교수의 인터랙션 _ 커뮤니케이션 코치','https://youtu.be/CjgefsNYBOI?si=OAmMjzTnTStDAsv1','https://img.youtube.com/vi/CjgefsNYBOI/hqdefault.jpg'),
            (27,'WORK','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',3,'영상','가치를 인정받는 발표 스피치 잘하는 방법! 업무 발표 스피치','에어클래스','https://www.youtube.com/watch?v=aqdQdePf2U0','https://img.youtube.com/vi/aqdQdePf2U0/hqdefault.jpg'),
            (28,'WORK','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',4,'아티클','내향인도 발표 고수로 만드는 직장인의 발표 스킬 10가지','퍼블리','https://publy.co/content/7527','https://newneek.imgix.net/images/2024/04/05/1712290495_VTm5zKFieuLkVVLMUTKEiW1tfkfWmraOdimBbVlM.jpeg?fm=pjpg'),
            (29,'WORK','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',5,'영상','문서와 프레젠테이션의 5단계','파워포인트 블루스','https://www.youtube.com/watch?v=OKOjWrv9fBQ','https://img.youtube.com/vi/OKOjWrv9fBQ/hqdefault.jpg'),
            (30,'WORK','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',6,'영상','10년차 디자이너가 말하는 프레젠테이션 디자인 원칙','에어클래스','https://www.youtube.com/watch?v=qq3xf5LUuWw','https://img.youtube.com/vi/qq3xf5LUuWw/hqdefault.jpg'),
            (31,'WORK','D_6_TO_3','발음과 속도를 점검해보세요',1,'영상','[프레젠테이션 시크릿] 발표 할 때 시간 관리하는 방법','콘텐츠위드','https://youtu.be/RjD5xT7ftME?si=sVzV7VhHyhMqdZip','https://img.youtube.com/vi/RjD5xT7ftME/hqdefault.jpg'),
            (32,'WORK','D_6_TO_3','발음과 속도를 점검해보세요',2,'영상','발표자의 위치와 발표시간 관리 기술, 플랫폼스킬','구은화 TV','https://www.youtube.com/watch?v=LGyOqO53mmk','https://img.youtube.com/vi/LGyOqO53mmk/hqdefault.jpg'),
            (33,'WORK','D_6_TO_3','발음과 속도를 점검해보세요',3,'영상','잘 들리게 말하는 법, 3가지','민지적 시점','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (34,'WORK','D_6_TO_3','발음과 속도를 점검해보세요',4,'아티클','내향인도 발표 고수로 만드는 직장인의 발표 스킬 10가지','퍼블리','https://publy.co/content/7527','https://newneek.imgix.net/images/2024/04/05/1712290495_VTm5zKFieuLkVVLMUTKEiW1tfkfWmraOdimBbVlM.jpeg?fm=pjpg'),
            (35,'WORK','D_6_TO_3','발음과 속도를 점검해보세요',5,'영상','가치를 인정받는 발표 스피치 잘하는 방법! 업무 발표 스피치','에어클래스','https://www.youtube.com/watch?v=aqdQdePf2U0','https://img.youtube.com/vi/aqdQdePf2U0/hqdefault.jpg'),
            (36,'WORK','D_6_TO_3','발음과 속도를 점검해보세요',6,'아티클','발표 잘하는 척하는 방법','사업개발자 일당백','https://brunch.co.kr/@junbd/16','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2F3DtW%2Fimage%2F4Bimj88S2YstFr_f5YIx8QbODgw.jpeg'),
            (37,'WORK','D_2_TO_1','실전처럼 말해보세요',1,'영상','발표 잘하는 법, 경쟁PT와 평가에서 질의응답 Q&A 기술','구은화 TV','https://www.youtube.com/watch?v=WmOXzfgsJho','https://img.youtube.com/vi/WmOXzfgsJho/hqdefault.jpg'),
            (38,'WORK','D_2_TO_1','실전처럼 말해보세요',2,'영상','발표 잘하려고 하지 마세요 - 수주 발표에서 깨달은 발표법','미지컬랩','https://www.youtube.com/watch?v=hXjnNOGDWc8','https://img.youtube.com/vi/hXjnNOGDWc8/hqdefault.jpg'),
            (39,'WORK','D_2_TO_1','실전처럼 말해보세요',3,'영상','프레젠테이션 발표자료 잘 만드는 방법 – 제안발표, IR피칭, 보고서 첫 장','구은화 TV','https://www.youtube.com/watch?v=9OjNpMzMjgI','https://img.youtube.com/vi/9OjNpMzMjgI/hqdefault.jpg'),
            (40,'WORK','D_2_TO_1','실전처럼 말해보세요',4,'아티클','내향인도 발표 고수로 만드는 직장인의 발표 스킬 10가지','퍼블리','https://publy.co/content/7527','https://newneek.imgix.net/images/2024/04/05/1712290495_VTm5zKFieuLkVVLMUTKEiW1tfkfWmraOdimBbVlM.jpeg?fm=pjpg'),
            (41,'WORK','D_2_TO_1','실전처럼 말해보세요',5,'아티클','발표 잘하는 척하는 방법','사업개발자 일당백','https://brunch.co.kr/@junbd/16','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2F3DtW%2Fimage%2F4Bimj88S2YstFr_f5YIx8QbODgw.jpeg'),
            (42,'WORK','D_2_TO_1','실전처럼 말해보세요',6,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','특수교육학 대학원생 노트','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (43,'WORK','D_DAY','발표 전 긴장을 완화해보세요',1,'영상','대중 앞에서 말 잘하는 법','스터디언','https://www.youtube.com/watch?v=nf0pfzoqbeA','https://img.youtube.com/vi/nf0pfzoqbeA/hqdefault.jpg'),
            (44,'WORK','D_DAY','발표 전 긴장을 완화해보세요',2,'영상','잘 들리게 말하는 법, 3가지','민지적 시점','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (45,'WORK','D_DAY','발표 전 긴장을 완화해보세요',3,'영상','[프레젠테이션 시크릿] 발표 할 때 시간 관리하는 방법','콘텐츠위드','https://youtu.be/RjD5xT7ftME?si=sVzV7VhHyhMqdZip','https://img.youtube.com/vi/RjD5xT7ftME/hqdefault.jpg'),
            (46,'WORK','D_DAY','발표 전 긴장을 완화해보세요',4,'아티클','내향인도 발표 고수로 만드는 직장인의 발표 스킬 10가지','퍼블리','https://publy.co/content/7527','https://newneek.imgix.net/images/2024/04/05/1712290495_VTm5zKFieuLkVVLMUTKEiW1tfkfWmraOdimBbVlM.jpeg?fm=pjpg'),
            (47,'WORK','D_DAY','발표 전 긴장을 완화해보세요',5,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','특수교육학 대학원생 노트','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (48,'WORK','D_DAY','발표 전 긴장을 완화해보세요',6,'영상','발표 전 떨리는 분들, 꼭 보셔야 합니다.','윤닥의 인지행동치료 클리닉','https://youtu.be/bSxnY64cD0w?si=_IZvYuO8QkkZO7r8','https://img.youtube.com/vi/bSxnY64cD0w/hqdefault.jpg'),
            (49,'OFFER','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',1,'아티클','설득되는 이유를 알아야 제대로 설득할 수 있다, 설득의 3요소','디베이트포올','https://debateforall.org/blog/?bmode=view&idx=4174060','https://cdn.imweb.me/upload/S201802015a72b9e2e606c/78865b1cf78f2.png'),
            (50,'OFFER','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',2,'아티클','설득을 잘하는 법','수지','https://brunch.co.kr/@shootst/35','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2F87n%2Fimage%2FdCMkN_qVyX1RF6LGKlSuWyGmWkw'),
            (51,'OFFER','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',3,'영상','정부지원금, IR, 투자유치제안서 발표자료 사례 분석','CEO 응급실 | 위너스랩TV','https://www.youtube.com/watch?v=IKBlZcS3_Ys','https://img.youtube.com/vi/IKBlZcS3_Ys/hqdefault.jpg'),
            (52,'OFFER','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',4,'영상','합격 확률 높여주는 발표 스피치 준비 꿀팁 - 정부지원사업 발표','스피치트레이닝','https://www.youtube.com/watch?v=oYtK3BFi8pQ','https://img.youtube.com/vi/oYtK3BFi8pQ/hqdefault.jpg'),
            (53,'OFFER','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',5,'영상','프레젠테이션 발표자료 잘 만드는 방법 – 제안발표, IR피칭, 보고서 첫 장','표현하다','https://www.youtube.com/watch?v=9OjNpMzMjgI','https://img.youtube.com/vi/9OjNpMzMjgI/hqdefault.jpg'),
            (54,'OFFER','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',6,'영상','10년차 디자이너가 말하는 프레젠테이션 디자인 원칙','에어클래스','https://www.youtube.com/watch?v=qq3xf5LUuWw','https://img.youtube.com/vi/qq3xf5LUuWw/hqdefault.jpg'),
            (55,'OFFER','D_6_TO_3','발음과 속도를 점검해보세요',1,'영상','투자 받고 싶다면? IR피칭, 발표 스피치 준비','에듀이너스','https://www.youtube.com/watch?v=9tqhd_N06lE','https://img.youtube.com/vi/9tqhd_N06lE/hqdefault.jpg'),
            (56,'OFFER','D_6_TO_3','발음과 속도를 점검해보세요',2,'영상','설득'' 하나로 인생역전한 노희영의 ''전략적으로 설득하는 법'' 3가지 비법 (연봉협상, PT꿀팁)','큰손 노희영','https://youtu.be/bsCxQbaDv3Q?si=gqDUZbj7EmQPp8LB','https://img.youtube.com/vi/bsCxQbaDv3Q/hqdefault.jpg'),
            (57,'OFFER','D_6_TO_3','발음과 속도를 점검해보세요',3,'영상','잘 들리게 말하는 법, 3가지','민지적 시점','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (58,'OFFER','D_6_TO_3','발음과 속도를 점검해보세요',4,'아티클','설득되는 이유를 알아야 제대로 설득할 수 있다, 설득의 3요소','디베이트포올','https://debateforall.org/blog/?bmode=view&idx=4174060','https://cdn.imweb.me/upload/S201802015a72b9e2e606c/78865b1cf78f2.png'),
            (59,'OFFER','D_6_TO_3','발음과 속도를 점검해보세요',5,'아티클','설득을 잘하는 법','수지','https://brunch.co.kr/@shootst/35','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2F87n%2Fimage%2FdCMkN_qVyX1RF6LGKlSuWyGmWkw'),
            (60,'OFFER','D_6_TO_3','발음과 속도를 점검해보세요',6,'영상','발음이 좋아지는 10가지 문장 / 발음교정 / 발음훈련','MODA TV','https://youtu.be/oR2crUMux0k?si=Tx0Nc8Gj1cddyAfZ','https://img.youtube.com/vi/oR2crUMux0k/hqdefault.jpg'),
            (61,'OFFER','D_2_TO_1','실전처럼 말해보세요',1,'영상','이것만 알면 여러 사람 앞에서도 말 잘할 수 있습니다 | 말하기, 발표, 데일 카네기','책식주의','https://youtu.be/oPoE8dfTybo?si=IMOyHLnFFQjechR0','https://img.youtube.com/vi/oPoE8dfTybo/hqdefault.jpg'),
            (62,'OFFER','D_2_TO_1','실전처럼 말해보세요',2,'영상','합격 확률 높여주는 발표 스피치 준비 꿀팁 - 정부지원사업 발표','표현하다','https://www.youtube.com/watch?v=oYtK3BFi8pQ','https://img.youtube.com/vi/oYtK3BFi8pQ/hqdefault.jpg'),
            (63,'OFFER','D_2_TO_1','실전처럼 말해보세요',3,'영상','정부지원금, IR, 투자유치제안서 발표자료 사례 분석','CEO 응급실 | 위너스랩TV','https://www.youtube.com/watch?v=IKBlZcS3_Ys','https://img.youtube.com/vi/IKBlZcS3_Ys/hqdefault.jpg'),
            (64,'OFFER','D_2_TO_1','실전처럼 말해보세요',4,'아티클','설득되는 이유를 알아야 제대로 설득할 수 있다, 설득의 3요소','디베이트포올','https://debateforall.org/blog/?bmode=view&idx=4174060','https://cdn.imweb.me/upload/S201802015a72b9e2e606c/78865b1cf78f2.png'),
            (65,'OFFER','D_2_TO_1','실전처럼 말해보세요',5,'아티클','말하기 훈련으로 설득력 UP! 당신의 의견이 통하는 비법 대공개','따뜻한 심리학','https://warmpsy.tistory.com/entry/%EB%A7%90%ED%95%98%EA%B8%B0-%ED%9B%88%EB%A0%A8%EC%9C%BC%EB%A1%9C-%EC%84%A4%EB%93%9D%EB%A0%A5-UP-%EB%8B%B9%EC%8B%A0%EC%9D%98-%EC%9D%98%EA%B2%AC%EC%9D%B4-%ED%86%B5%ED%95%98%EB%8A%94-%EB%B9%84%EB%B2%95-%EB%8C%80%EA%B3%B5%EA%B0%9C','https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdna%2F0926J%2FbtsNW0Q6FGj%2FAAAAAAAAAAAAAAAAAAAAAAoyIEJ51BQ7mE8W1P7D-e5eZ16_s8r1r4MIcvFnsTzv%2Fimg.webp%3Fcredential%3DyqXZFxpELC7KVnFOS48ylbz2pIh7yKj8%26expires%3D1782831599%26allow_ip%3D%26allow_referer%3D%26signature%3DS0WybMsskwF%252BatWGVwWH4vImjdY%253D'),
            (66,'OFFER','D_2_TO_1','실전처럼 말해보세요',6,'영상','대중 앞에서 말 잘하는 법','스터디언','https://www.youtube.com/watch?v=nf0pfzoqbeA','https://img.youtube.com/vi/nf0pfzoqbeA/hqdefault.jpg'),
            (67,'OFFER','D_DAY','발표 전 긴장을 완화해보세요',1,'영상','많은 사람들 앞에서 긴장 안하고 말 잘하는 법 (딱 3가지만 기억하세요)','영감 수업','https://youtu.be/yvtw71vU0iA?si=XBCEaMOy3_nMF0W5','https://img.youtube.com/vi/yvtw71vU0iA/hqdefault.jpg'),
            (68,'OFFER','D_DAY','발표 전 긴장을 완화해보세요',2,'영상','잘 들리게 말하는 법, 3가지','스피치 강의','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (69,'OFFER','D_DAY','발표 전 긴장을 완화해보세요',3,'영상','투자 받고 싶다면? IR피칭, 발표 스피치 준비','에듀이너스','https://www.youtube.com/watch?v=9tqhd_N06lE','https://img.youtube.com/vi/9tqhd_N06lE/hqdefault.jpg'),
            (70,'OFFER','D_DAY','발표 전 긴장을 완화해보세요',4,'아티클','설득되는 이유를 알아야 제대로 설득할 수 있다, 설득의 3요소','디베이트포올','https://debateforall.org/blog/?bmode=view&idx=4174060','https://cdn.imweb.me/upload/S201802015a72b9e2e606c/78865b1cf78f2.png'),
            (71,'OFFER','D_DAY','발표 전 긴장을 완화해보세요',5,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','특수교육학 대학원생 노트','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (72,'OFFER','D_DAY','발표 전 긴장을 완화해보세요',6,'아티클','스피치, 발표 잘하는 법 - 오프닝은 어떻게?','리얼디베이트','https://brunch.co.kr/@realdebate/56','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2FCjd%2Fimage%2F7GIlVIF6jUzI0K1Mdl92Ao_h78g'),
            (73,'EVENT','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',1,'영상','행사 사회 잘보는 방법, 진행자 사회자 멘트','구은화 TV','https://www.youtube.com/watch?v=9Ez_2Wz-Zc8','https://img.youtube.com/vi/9Ez_2Wz-Zc8/hqdefault.jpg'),
            (74,'EVENT','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',2,'영상','행사 진행 멘트 어떻게? 사회 잘 보는 방법, 피할 수 없는 결혼식 송년회 mc 사회 본다면?/말버스','말버스','https://youtu.be/Zg5zL1aRR7Y?si=6fXjK_IHL0j4aSk4','https://img.youtube.com/vi/Zg5zL1aRR7Y/hqdefault.jpg'),
            (75,'EVENT','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',3,'영상','진행 사회자 멘트, 어떻게 말해야 쉽고 재밌게 말할 수 있을까','살리는 TV- 말로 인생을 회복하는 채널','https://www.youtube.com/watch?v=HEw9WrD6tdM','https://img.youtube.com/vi/HEw9WrD6tdM/hqdefault.jpg'),
            (76,'EVENT','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',4,'아티클','발표 스피치, 어떻게 하면 잘 할 수 있을까요?','교육부 공식 블로그','https://if-blog.tistory.com/11177','https://blog.kakaocdn.net/dna/bf2ULN/btqJOz6sOnf/AAAAAAAAAAAAAAAAAAAAAGX6OE2jt0ZYNKWyVfsxBu9FSFhvfQBAHqU8UVTMrXet/img.png?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=w2yDrlTlWsLCPgovnGuR5VTU%2FVY%3D'),
            (77,'EVENT','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',5,'아티클','스피치, 발표 잘하는 법 - 오프닝은 어떻게?','리얼디베이트','https://brunch.co.kr/@realdebate/56','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2FCjd%2Fimage%2F7GIlVIF6jUzI0K1Mdl92Ao_h78g'),
            (78,'EVENT','D_7_PLUS','발표 흐름을 키워드별로 정리해보세요',6,'영상','발표자의 위치와 발표시간 관리 기술, 플랫폼스킬','구은화 TV','https://www.youtube.com/watch?v=LGyOqO53mmk','https://img.youtube.com/vi/LGyOqO53mmk/hqdefault.jpg'),
            (79,'EVENT','D_6_TO_3','발음과 속도를 점검해보세요',1,'영상','어떤 행사에서도 적용가능한 사회 잘 보는 방법 3가지','박은주 아나운서스피치','https://www.youtube.com/watch?v=qJ1VigDprLs','https://img.youtube.com/vi/qJ1VigDprLs/hqdefault.jpg'),
            (80,'EVENT','D_6_TO_3','발음과 속도를 점검해보세요',2,'영상','행사MC 멘트대본쓰는법, 축제사회보는법','박은주 아나운서스피치','https://www.youtube.com/watch?v=PMzbuSjap9Y','https://img.youtube.com/vi/PMzbuSjap9Y/hqdefault.jpg'),
            (81,'EVENT','D_6_TO_3','발음과 속도를 점검해보세요',3,'영상','발음이 좋아지는 10가지 문장 / 발음교정 / 발음훈련','MODA TV','https://youtu.be/oR2crUMux0k?si=Tx0Nc8Gj1cddyAfZ','https://img.youtube.com/vi/oR2crUMux0k/hqdefault.jpg'),
            (82,'EVENT','D_6_TO_3','발음과 속도를 점검해보세요',4,'영상','잘 들리게 말하는 법, 3가지','민지적 시점','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (83,'EVENT','D_6_TO_3','발음과 속도를 점검해보세요',5,'아티클','발표 스피치, 어떻게 하면 잘 할 수 있을까요?','교육부 공식 블로그','https://if-blog.tistory.com/11177','https://blog.kakaocdn.net/dna/bf2ULN/btqJOz6sOnf/AAAAAAAAAAAAAAAAAAAAAGX6OE2jt0ZYNKWyVfsxBu9FSFhvfQBAHqU8UVTMrXet/img.png?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=w2yDrlTlWsLCPgovnGuR5VTU%2FVY%3D'),
            (84,'EVENT','D_6_TO_3','발음과 속도를 점검해보세요',6,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','특수교육학 대학원생 노트','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (85,'EVENT','D_2_TO_1','실전처럼 말해보세요',1,'영상','행사 사회 잘보는 방법, 진행자 사회자 멘트','구은화 TV','https://www.youtube.com/watch?v=9Ez_2Wz-Zc8','https://img.youtube.com/vi/9Ez_2Wz-Zc8/hqdefault.jpg'),
            (86,'EVENT','D_2_TO_1','실전처럼 말해보세요',2,'영상','사회보는법, 사회자 멘트와 대본, 행사MC 학원 샘플 강의','박은주 아나운서스피치','https://www.youtube.com/watch?v=11pA2PNWe-A','https://img.youtube.com/vi/11pA2PNWe-A/hqdefault.jpg'),
            (87,'EVENT','D_2_TO_1','실전처럼 말해보세요',3,'영상','이것만 알면 여러 사람 앞에서도 말 잘할 수 있습니다 | 말하기, 발표, 데일 카네기','책식주의','https://youtu.be/oPoE8dfTybo?si=IMOyHLnFFQjechR0','https://img.youtube.com/vi/oPoE8dfTybo/hqdefault.jpg'),
            (88,'EVENT','D_2_TO_1','실전처럼 말해보세요',4,'아티클','스피치, 발표 잘하는 법 - 오프닝은 어떻게?','리얼디베이트','https://brunch.co.kr/@realdebate/56','https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http%3A%2F%2Ft1.daumcdn.net%2Fbrunch%2Fservice%2Fuser%2FCjd%2Fimage%2F7GIlVIF6jUzI0K1Mdl92Ao_h78g'),
            (89,'EVENT','D_2_TO_1','실전처럼 말해보세요',5,'아티클','떨지 않고 발표 잘 하는 법(말, 프레젠테이션)','특수교육학 대학원생 노트','https://sp-edu.tistory.com/12','https://blog.kakaocdn.net/dna/HFrdl/btsA7L1p6SX/AAAAAAAAAAAAAAAAAAAAADB233Z8fzckudG3ptOKTYcVYBJAuaXNF4ySyjllpsic/img.jpg?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=Jc7aPXsu%2BV8uZOSjY1h%2F5hcIjk8%3D'),
            (90,'EVENT','D_2_TO_1','실전처럼 말해보세요',6,'영상','[프레젠테이션 시크릿] 발표 할 때 시간 관리하는 방법','콘텐츠위드','https://youtu.be/RjD5xT7ftME?si=sVzV7VhHyhMqdZip','https://img.youtube.com/vi/RjD5xT7ftME/hqdefault.jpg'),
            (91,'EVENT','D_DAY','발표 전 긴장을 완화해보세요',1,'영상','대중 앞에서 말 잘하는 법','스터디언','https://www.youtube.com/watch?v=nf0pfzoqbeA','https://img.youtube.com/vi/nf0pfzoqbeA/hqdefault.jpg'),
            (92,'EVENT','D_DAY','발표 전 긴장을 완화해보세요',2,'영상','진행 사회자 멘트, 어떻게 말해야 쉽고 재밌게 말할 수 있을까','살리는 TV- 말로 인생을 회복하는 채널','https://www.youtube.com/watch?v=HEw9WrD6tdM','https://img.youtube.com/vi/HEw9WrD6tdM/hqdefault.jpg'),
            (93,'EVENT','D_DAY','발표 전 긴장을 완화해보세요',3,'영상','잘 들리게 말하는 법, 3가지','스피치 강의','https://www.youtube.com/watch?v=UqIOXiT3PZw','https://img.youtube.com/vi/UqIOXiT3PZw/hqdefault.jpg'),
            (94,'EVENT','D_DAY','발표 전 긴장을 완화해보세요',4,'영상','발표 전 떨리는 분들, 꼭 보셔야 합니다.','윤닥의 인지행동치료 클리닉','https://youtu.be/bSxnY64cD0w?si=_IZvYuO8QkkZO7r8','https://img.youtube.com/vi/bSxnY64cD0w/hqdefault.jpg'),
            (95,'EVENT','D_DAY','발표 전 긴장을 완화해보세요',5,'아티클','긴장 푸는 법 - 간단하고 효과적인 3가지','DT당톡','https://dangtalk.co.kr/%EC%95%84%EB%82%98%EC%9A%B4%EC%84%9C%EA%B0%80-%EC%A7%81%EC%A0%91-%EC%95%8C%EB%A0%A4%EC%A3%BC%EB%8A%94-%EB%B0%9C%ED%91%9C%EA%B8%B4%EC%9E%A5-%ED%95%B4%EC%86%8C%EC%99%80-%EB%AA%A9%EC%86%8C%EB%A6%AC/','https://dangtalk.co.kr/wp-content/uploads/2025/02/%EB%B0%9C%ED%91%9C%EB%B6%88%EC%95%88-%EB%AA%A9%EC%86%8C%EB%A6%AC%ED%8A%B8%EB%A0%88%EC%9D%B4%EB%8B%9D-%EC%B9%BC%EB%9F%BC-%EC%8D%B8%EB%84%A4%EC%9D%BC.jpg'),
            (96,'EVENT','D_DAY','발표 전 긴장을 완화해보세요',6,'아티클','발표 스피치, 어떻게 하면 잘 할 수 있을까요?','교육부 공식 블로그','https://if-blog.tistory.com/11177','https://blog.kakaocdn.net/dna/bf2ULN/btqJOz6sOnf/AAAAAAAAAAAAAAAAAAAAAGX6OE2jt0ZYNKWyVfsxBu9FSFhvfQBAHqU8UVTMrXet/img.png?allow_ip=&allow_referer=&credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1782831599&signature=w2yDrlTlWsLCPgovnGuR5VTU%2FVY%3D');
        """;

        jdbcTemplate.execute(insertSql);
        log.info("큐레이션 데이터 96건 삽입 완료");

        return ResponseEntity.ok("큐레이션 테이블 생성 및 96개 데이터 성공적으로 삽입되었습니다! 🚀");
    }
}