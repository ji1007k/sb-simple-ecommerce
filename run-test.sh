#!/bin/bash

echo "==================================="
echo "    이커머스 동시성 테스트 실행"
echo "==================================="
echo

echo "[1] 전체 테스트 실행 중..."
./gradlew test --tests ConcurrencyTest

echo
echo "[2] 테스트 결과 확인"
echo

echo "테스트가 완료되었습니다!"
echo "자세한 로그는 build/reports/tests/test/index.html 에서 확인하세요."
