package com.oppocts.ui

import android.app.Activity
import android.os.Bundle
import com.oppocts.trigger.CTSTrigger

/**
 * 전용 트리거 액티비티.
 * 실행되는 즉시 MiCTS 방식의 네이티브 서비스 트리거를 수행하고 종료합니다.
 * 외부 버튼 매퍼 앱에서 이 액티비티를 연결하여 사용할 수 있습니다.
 */
class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 투명하게 처리하기 위해 뷰를 생략하거나 최소화
        
        // 트리거 실행
        CTSTrigger.triggerViaNativeService(this)
        
        // 즉시 종료
        finish()
    }
}
