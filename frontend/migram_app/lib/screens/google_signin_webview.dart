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
    controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (NavigationRequest request) {
            // 로그인 성공 후 리다이렉트 URL 확인
            if (request.url.contains('loginSuccess')) {
              Navigator.of(context).pop(true); // 성공
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
          onPageFinished: (String url) {
            if (url.contains('error=true')) {
              Navigator.of(context).pop(false); // 실패
            }
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.loginUrl));
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
