import 'package:flutter/material.dart';
import 'package:migram_app/screens/main_screen.dart';
import 'package:migram_app/services/dio_service.dart';

class CountrySelectionScreen extends StatefulWidget {
  const CountrySelectionScreen({super.key});

  @override
  State<CountrySelectionScreen> createState() => _CountrySelectionScreenState();
}

class _CountrySelectionScreenState extends State<CountrySelectionScreen> {
  String? selectedOriginCountry;
  String? selectedCurrentCountry;
  final List<String> countries = [
    'Korea', 'Japan', 'China', 'USA', 'UK', 'France', 'Germany'
    // 필요한 국가들 추가
  ];

  Future<void> _saveCountries() async {
    try {
      if (selectedOriginCountry != null && selectedCurrentCountry != null) {
        final dioService = DioService();
        await dioService.put(
          '/api/user/countries',
          data: {
            'originCountry': selectedOriginCountry,
            'currentCountry': selectedCurrentCountry,
          },
        );

        if (mounted) {
          Navigator.pushReplacement(
            context,
            MaterialPageRoute(builder: (context) => const MainScreen()),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('국가 정보 저장 중 오류가 발생했습니다')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('국가 선택'),
        automaticallyImplyLeading: false, // 뒤로가기 버튼 숨기기
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              '출신 국가와 현재 거주 국가를 선택해주세요',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            DropdownButtonFormField<String>(
              value: selectedOriginCountry,
              decoration: const InputDecoration(
                labelText: '출신 국가',
                border: OutlineInputBorder(),
              ),
              items: countries.map((String country) {
                return DropdownMenuItem(
                  value: country,
                  child: Text(country),
                );
              }).toList(),
              onChanged: (String? newValue) {
                setState(() {
                  selectedOriginCountry = newValue;
                });
              },
            ),
            const SizedBox(height: 20),
            DropdownButtonFormField<String>(
              value: selectedCurrentCountry,
              decoration: const InputDecoration(
                labelText: '현재 거주 국가',
                border: OutlineInputBorder(),
              ),
              items: countries.map((String country) {
                return DropdownMenuItem(
                  value: country,
                  child: Text(country),
                );
              }).toList(),
              onChanged: (String? newValue) {
                setState(() {
                  selectedCurrentCountry = newValue;
                });
              },
            ),
            const SizedBox(height: 30),
            ElevatedButton(
              onPressed: (selectedOriginCountry != null &&
                      selectedCurrentCountry != null)
                  ? _saveCountries
                  : null,
              child: const Text('저장'),
            ),
          ],
        ),
      ),
    );
  }
}
