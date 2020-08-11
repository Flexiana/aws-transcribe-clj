(ns aws-transcribe-clj.facebook.ui
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [aws-transcribe-clj.facebook.events :as fe]))

(defn login-button []
  (let [container-id "login-button-root"]
   (r/create-class
     {:display-name           "facebook-login-button"
      :component-did-mount    #(rf/dispatch [::fe/refresh-login-button container-id])
      :component-did-update   #(rf/dispatch [::fe/refresh-login-button container-id])
      :component-will-unmount #(rf/dispatch [::fe/deactivate-login])
      :reagent-render         (fn []
                                [:div {:id container-id}
                                 [:div {:id "fb-root"} nil]
                                 [:div {:class                "fb-login-button"
                                        :data-size            "large"
                                        :data-button-type     "continue_with"
                                        :data-layout          "default"
                                        :data-use-continue-as "true"}
                                  nil]])})))
