(ns status-dapp.views
  (:require-macros [status-dapp.utils :refer [defview letsubs]])
  (:require [status-dapp.react-native-web :as react]
            [re-frame.core :as re-frame]
            [status-dapp.components :as ui]
            [status-dapp.constants :as constants]))

(defn no-web3 []
  [react/view {:style {:flex 1 :padding 10 :align-items :center :justify-content :center}}
   [react/text {:style {:font-weight :bold}}
    "Can't find web3 library"]])

(defview contract-panel [accounts]
  (letsubs [{:keys [tx-hash address value]} [:get :contract]]
    [react/view
     [react/view {:style {:margin-bottom 10}}
      [ui/button "Sign message" #(re-frame/dispatch [:sign-message])]]
     (cond

       address
       [react/view
        [ui/label "Contract deployed at: " ""]
        [react/text address]

        [ui/button "Call contract get function" #(re-frame/dispatch [:contract-call-get])]
        [react/text "Default value: 0"]
        (when value
          [react/text value])

        [ui/button "Call contract set function" #(re-frame/dispatch [:contract-call-set 1])]
        [react/text "Sets value to 1"]

        [ui/button "Call function 2 times in a row" #(do
                                                       (re-frame/dispatch [:contract-call-set 10])
                                                       (re-frame/dispatch [:contract-call-set 20]))]
        [react/text "First tx sets value to 10, second to 20"]]

       tx-hash
       [react/view {:style {:padding-top 10}}
        [react/activity-indicator {:animating true}]
        [react/text {:selectable true} (str "Mining new contract in tx: " tx-hash)]]

       :else
       [ui/button "Deploy simple contract" #(re-frame/dispatch [:deploy-contract (str (first accounts))])])]))

(defview web3-view []
  (letsubs [{:keys [api node network ethereum whisper accounts syncing gas-price
                    default-account coinbase default-block]}
            [:get :web3-async-data]
            web3 [:get :web3]
            tab-view [:get :tab-view]
            balances [:get :balances]]
    [react/view {:style {:flex 1}}
     [ui/tab-buttons tab-view]
     [react/scroll-view {:style {:flex 1}}
      [react/view {:style {:flex 1 :padding 10}}

       (when (= :assets tab-view)
         [react/view
          ;;TODO CORS
          ;;[ui/button "Request Ropsten ETH" #(re-frame/dispatch [:request-ropsten-eth (str (first accounts))])]
          ;;[react/view {:style {:width 5}}]
          (if (= "3" network)
            [react/view
             [ui/asset-button "STT" constants/stt-ropsten-contract]
             [ui/asset-button "ATT" constants/att-ropsten-contract]
             [ui/asset-button "HND" constants/hnd-ropsten-contract]
             [ui/asset-button "LXS" constants/lxs-ropsten-contract]
             [ui/asset-button "ADI" constants/adi-ropsten-contract]
             [ui/asset-button "WGN" constants/wgn-ropsten-contract]
             [ui/asset-button "MDS" constants/mds-ropsten-contract]]
            [react/text "Assets supported only in Ropsten Testnet"])])

       (when (= :transactions tab-view)
         [contract-panel accounts])

       (when (= :version tab-view)
         [react/view
          [react/text {:style {:font-weight :bold :margin-top 20}} "Version"]
          [ui/label "api" api]
          [ui/label "node" node]
          [ui/label "network" (str network " (" (or (constants/chains network) "Unknown") ")")]
          [ui/label "ethereum" ethereum]
          [ui/label "whisper" whisper]])

       (when (= :accounts tab-view)
         [react/view
          [react/text {:style {:font-weight :bold :margin-top 20}} "Accounts"]
          [ui/label "defaultAccount" ""]
          [react/text default-account]
          [ui/label "coinbase" ""]
          [react/text coinbase]
          [ui/label "accounts" ""]
          (for [account accounts]
            ^{:key account}
            [react/view
             [react/text account]
             [ui/button "Get balance" #(re-frame/dispatch [:get-balance account])]
             (when (get balances account)
               [react/text (str "Balance: " (get balances account) " wei")])])])

       (when (= :eth tab-view)
         [react/view
          [react/text {:style {:font-weight :bold :margin-top 20}} "Eth"]
          [ui/label "defaultBlock" default-block]
          (if syncing
            [react/view
             [ui/label "isSyncing" "true"]
             [ui/label "startingBlock" (.-startingBlock syncing)]
             [ui/label "currentBlock" (.-currentBlock syncing)]
             [ui/label "highestBlock" (.-highestBlock syncing)]]
            [ui/label "isSyncing" "false"])
          (when gas-price
            [ui/label "gasPrice" (str (.toString gas-price 10) " wei")])])

       (when (= :about tab-view)
         [react/view
          [react/view {:style {:flex-direction :row :padding-vertical 10}}
           [react/text "web3 provider: "]
           (cond (.-currentProvider.isStatus web3)
                 [react/text "Status"]
                 (.-currentProvider.isMetaMask web3)
                 [react/text "MetaMask"]
                 :else [react/text "Unknown"])]

          [react/text "Simple DApp"]
          [react/text {:selectable true} "Sources: https://github.com/status-im/status-dapp"]])]]]))

(defview main []
  (letsubs [view-id [:get :view-id]]
    (case view-id
      :web3 [web3-view]
      [no-web3])))