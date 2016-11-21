
MultipleControllers_FailureRecovery

概要
　2連結グラフのネットワークに対して、サイクル構造を用いた障害復旧方式を適用させます。単一・複数コントローラに対応可能です。障害復旧の動作の流れは以下のようになります。
 
1.コントローラに保持されているトポロジ情報を取得
2.トポロジ情報からグラフを作成
3.グラフからタイセットを作成
4.タイセットからタイセットグラフを作成
5.フローエントリを作るために必要な情報を整理（最短経路、次タイセット、出力ポート、タイセットIDの算出）
6.タイセットノード、エッジノード、境界ノード、コアノードのフローエントリの作成と追加

使い方
プログラムを実行するまえに、mininetでネットワークを構築する必要があります。また、Mainクラスのソースコードを実験環境によって書き換える必要があります。

1.mininetで2連結グラフのネットワークを作成し、スイッチをコントローラに割り当てる。
2.Mainクラスのフィールドにある「各コントローラのIPアドレス」（変数）にコントローラのIPアドレスを入力する。
3.プログラムを実行するとフローエントリが追加され、追加が終わると「完了」と表示される。

障害復旧動作テスト
1.フローエントリの追加後、pingで疎通ができることを確認（現用経路が使用されている）
2.スイッチ間のリンクをダウンさせ、pingで疎通ができることを確認（迂回経路が使用されている）

クラス名
機能

Mainクラス
各コントローラのIPアドレスからフローエントリ追加までの一連の流れを行う。

Topologyクラス
各コントローラのIPアドレスからトポロジ情報の取得とグラフの作成を行う。ノードへ情報を追加する機能も担う。

TopologyInfoクラス
各コントローラのトポロジ情報を保持する

MakeTiesetクラス
グラフからタイセットを作成する。次数が最大となるノードを始点としてBFSを用いて作成している。

Tiesetクラス
タイセットの情報を保持する

TiesetEdgeクラス
タイセットグラフのエッジのクラス

TiesetGraphクラス
タイセットからタイセットグラフを作成する。タイセットグラフは、ノードを共有しているタイセット間でエッジを形成することで作成しているため、厳密にいうとタイセットグラフというより、サイクルグラフを作成している。

MakeFlowクラス
フローエントリを作成するクラスの親クラス。フローエントリを作成するためのフィールドとメソッドが記載されている。

MakeXMLクラス
フローエントリの情報をXML形式で作成するためのクラス。XMLの作成は、JDOMを用いており、XMLを動的に作れる点でグループテーブルで多重障害を想定した障害復旧の実装がやりやすいと思います。

MakeURIクラス
REST APIを使うためのURIが記載されている。フローエントリ追加の際に用いられる。

RestClientクラス
RESTリクエストを行うためのクライアントクラス

RestRequestクラス
RESTリクエストを行うためのクラス

TiesetNodeFlowクラス
タイセットノードへフローエントリを追加するためのクラス

BorderNodeFlowクラス
タイセット境界ノードへフローエントリを追加するためのクラス

EdgeNodeFlowクラス
エッジノードへフローエントリを追加するためのクラス

CoreNodeFlowクラス
コアノードへフローエントリを追加するためのクラス

Dropクラス
デフォルトのフローエントリが使われないようにフローを破棄するアクションを記載したフローエントリの追加
デフォルトのフローエントリのプライオリティより高く設定する。

Nodeクラス
ノード情報の保持

Edgeクラス（未実装）
エッジ情報の保持
