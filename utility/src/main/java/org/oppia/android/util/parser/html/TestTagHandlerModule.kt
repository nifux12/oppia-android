package org.oppia.android.util.parser.html

import android.view.View
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class TestTagHandlerModule {
  @Provides
  @Singleton
  fun provideConceptCardListener(): ConceptCardTagHandler.ConceptCardLinkClickListener {
    return object : ConceptCardTagHandler.ConceptCardLinkClickListener {
      override fun onConceptCardLinkClicked(view: View, skillId: String) {}
    }
  }
}
