// screens/country_selection_screen.dart
import 'package:flutter/material.dart';
import '../services/dio_service.dart';
import 'main_screen.dart';

class CountrySelectionScreen extends StatefulWidget {
  const CountrySelectionScreen({super.key});

  @override
  State<CountrySelectionScreen> createState() => _CountrySelectionScreenState();
}

class _CountrySelectionScreenState extends State<CountrySelectionScreen> {
  String? selectedOriginCountry;
  String? selectedCurrentCountry;
  List<String> countries = []; // 실제로는 API에서 가져온 국가 목록을 사용

  @override
  void initState() {
    super.initState();
    _loadCountries();
  }

  Future<void> _loadCountries() async {
    try {
      final response = await DioService().get('/api/countries');
      setState(() {
        countries = List<String>.from(response.data);
      });
    } catch (e) {
      print('Error loading countries: $e');
    }
  }

  Future<void> _saveCountries() async {
    if (selectedOriginCountry == null || selectedCurrentCountry == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('출신 국가와 현재 국가를 모두 선택해주세요')),
      );
      return;
    }

    try {
      await DioService().post('/api/user/countries', data: {
        'originCountry': selectedOriginCountry,
        'currentCountry': selectedCurrentCountry,
      });

      if (context.mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (context) => const MainScreen()),
        );
      }
    } catch (e) {
      print('Error saving countries: $e');
      if (context.mounted) {
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
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '출신 국가',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            DropdownButton<String>(
              isExpanded: true,
              hint: const Text('출신 국가를 선택하세요'),
              value: selectedOriginCountry,
              onChanged: (value) {
                setState(() {
                  selectedOriginCountry = value;
                });
              },
              items: countries.map((country) {
                return DropdownMenuItem<String>(
                  value: country,
                  child: Text(country),
                );
              }).toList(),
            ),
            const SizedBox(height: 24),
            const Text(
              '현재 국가',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            DropdownButton<String>(
              isExpanded: true,
              hint: const Text('현재 국가를 선택하세요'),
              value: selectedCurrentCountry,
              onChanged: (value) {
                setState(() {
                  selectedCurrentCountry = value;
                });
              },
              items: countries.map((country) {
                return DropdownMenuItem<String>(
                  value: country,
                  child: Text(country),
                );
              }).toList(),
            ),
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _saveCountries,
                child: const Text('저장'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
