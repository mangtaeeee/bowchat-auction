-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS product_service;
CREATE SCHEMA IF NOT EXISTS auction_service;
CREATE SCHEMA IF NOT EXISTS chat_service;

-- ShedLock 테이블 (각 서비스 스키마에)
CREATE TABLE IF NOT EXISTS user_service.shedlock (
                                                     name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
    );

CREATE TABLE IF NOT EXISTS product_service.shedlock (
                                                        name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
    );

CREATE TABLE IF NOT EXISTS auction_service.shedlock (
                                                        name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
    );