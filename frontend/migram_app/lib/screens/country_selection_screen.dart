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
  List<Map<String, String>> countries = []; // Map으로 변경
  bool isLoading = true;

  Future<void> _loadCountries() async {
    try {
      print('Loading countries...');
      final response = await DioService().get('/api/countries');
      print('Countries response: ${response.data}');

      final countryList = response.data as List;
      final countryData = <Map<String, String>>[];

      for (var country in countryList) {
        if (country is Map<String, dynamic>) {
          countryData.add({
            'code': country['code'] as String,
            'displayName': country['displayName'] as String,
          });
        }
      }

      // displayName으로 정렬
      countryData
          .sort((a, b) => a['displayName']!.compareTo(b['displayName']!));

      setState(() {
        countries = countryData;
        isLoading = false;
      });
    } catch (e) {
      print('Error loading countries: $e');
      setState(() {
        isLoading = false;
      });
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
      // enum 코드로 전송
      final requestData = {
        'originCountry': selectedOriginCountry, // 이제 enum 코드
        'currentCountry': selectedCurrentCountry, // 이제 enum 코드
      };
      print('Sending country data: $requestData');

      await DioService().put('/api/user/countries', data: requestData);

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
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : Padding(
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
                        value: country['code'], // enum 코드를 value로 사용
                        child: Text(country['displayName']!), // 표시명은 그대로
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
                        value: country['code'], // enum 코드를 value로 사용
                        child: Text(country['displayName']!), // 표시명은 그대로
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
