import os

# List of file paths to aggregate
file_paths = [
    #r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\AndroidManifest.xml',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\billing\BillingRepository.kt',
        r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\components\MapWithPathAndCircle.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\components\SettingsButton.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\EmailVerificationScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\EpisodeScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\EpisodesScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\HomeScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\IntroScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\LogInScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\MfaEnrollmentScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\SettingsScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\SignUpScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\screens\TfaScreen.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\theme\Theme.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\ui\theme\Type.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\viewmodel\ViewModelForHistoryWalkI.kt',
    r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\src\main\java\com\history_walk\history_walk_i\MainActivity.kt',
    #r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\build.gradle.kts',
    #r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\app\build.gradle.kts'
]

def aggregate_files(file_paths):
    aggregated_content = ""
    for path in file_paths:
        aggregated_content += f"=== {path} ===\n"
        try:
            with open(path, 'r', encoding='utf-8') as file:
                content = file.read()
                aggregated_content += content + "\n\n"
        except FileNotFoundError:
            aggregated_content += f"Error: File not found: {path}\n\n"
        except Exception as e:
            aggregated_content += f"Error reading {path}: {e}\n\n"
    return aggregated_content

def main():
    aggregated_string = aggregate_files(file_paths)
    
    # Optionally, you can save the aggregated string to a new file
    output_path = r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I\aggregated_contents.txt'
    try:
        with open(output_path, 'w', encoding='utf-8') as output_file:
            output_file.write(aggregated_string)
        print(f"Aggregated content successfully saved to {output_path}")
    except Exception as e:
        print(f"Failed to write aggregated content to file: {e}")
    
    # If you prefer to just have the aggregated string in the script, you can print it
    # print(aggregated_string)

if __name__ == "__main__":
    main()