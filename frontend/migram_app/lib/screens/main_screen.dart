import 'package:flutter/material.dart';
import 'feed_screen.dart';
import 'chat_rooms_screen.dart';
import 'profile_screen.dart';
import '../services/chat_service.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;
  final ChatService _chatService = ChatService();
  int _chatUnreadCount = 0;

  final List<Widget> _screens = const [
    FeedScreen(),
    ChatRoomsScreen(),
    ProfileScreen(),
  ];

  @override
  void initState() {
    super.initState();
    _listenToUnreadCount();
  }

  void _listenToUnreadCount() {
    _chatService.unreadCountStream?.listen((count) {
      if (mounted) {
        setState(() {
          _chatUnreadCount = count;
        });
      }
    });
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_selectedIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
        items: [
          const BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: '홈',
          ),
          BottomNavigationBarItem(
            icon: _chatUnreadCount > 0
                ? Badge(
                    label: Text('$_chatUnreadCount'),
                    child: const Icon(Icons.chat),
                  )
                : const Icon(Icons.chat),
            label: '채팅',
          ),
          const BottomNavigationBarItem(
            icon: Icon(Icons.person),
            label: '프로필',
          ),
        ],
      ),
    );
  }
}
