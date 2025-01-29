import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class GoogleSignInWebView extends StatefulWidget {
  final String loginUrl;

  const GoogleSignInWebView({
    super.key,
    required this.loginUrl,
  });

  @override
  State<GoogleSignInWebView> createState() => _GoogleSignInWebViewState();
}

class _GoogleSignInWebViewState extends State<GoogleSignInWebView> {
  late final WebViewController controller;

  @override
  void initState() {
    super.initState();
    print('Loading URL: ${widget.loginUrl}'); // URL 디버깅용

    controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      // User Agent 설정 추가
      ..setUserAgent(
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (NavigationRequest request) {
            print('Navigation request to: ${request.url}'); // 네비게이션 디버깅용
            if (request.url.contains('loginSuccess')) {
              Navigator.of(context).pop(true);
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
          onPageFinished: (String url) {
            if (url.contains('error=true')) {
              Navigator.of(context).pop(false);
            }
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.loginUrl)); // widget.loginUrl을 직접 사용
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Google 로그인'),
      ),
      body: WebViewWidget(controller: controller),
    );
  }
}
