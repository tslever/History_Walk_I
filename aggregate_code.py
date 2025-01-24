import os
import re  # Import the regular expressions module

# Define the common base path
base_path = r'C:\Users\thoma\AndroidStudioProjects\History_Walk_I'

# List of file paths relative to the base_path
file_paths = [
    r'app\src\main\AndroidManifest.xml',
    r'app\src\main\java\com\history_walk\history_walk_i\billing\BillingRepository.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\extensions\TasksExt.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\components\MapWithPathAndCircle.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\components\PathPoint.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\components\PathPointsLoader.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\components\SettingsButton.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\screens\EpisodeScreen.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\screens\EpisodesScreen.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\screens\HomeScreen.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\screens\IntroScreen.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\screens\SettingsScreen.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\theme\Theme.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\ui\theme\Type.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\viewmodel\ViewModelForHistoryWalkI.kt',
    r'app\src\main\java\com\history_walk\history_walk_i\MainActivity.kt',
    r'build.gradle.kts',
    r'app\build.gradle.kts'
]

def aggregate_files(base_path, relative_paths):
    aggregated_content = ""
    for relative_path in relative_paths:
        # Construct the full path by joining base_path with the relative_path
        full_path = os.path.join(base_path, relative_path)
        aggregated_content += f"=== {full_path} ===\n"
        try:
            with open(full_path, 'r', encoding='utf-8') as file:
                content = file.read()
                aggregated_content += content + "\n\n"
        except FileNotFoundError:
            aggregated_content += f"Error: File not found: {full_path}\n\n"
        except Exception as e:
            aggregated_content += f"Error reading {full_path}: {e}\n\n"
    return aggregated_content

def replace_import_groups(content):
    """
    Replace groups of lines starting with 'import' with '...'.
    
    Args:
        content (str): The aggregated content string.
        
    Returns:
        str: The modified content with import groups replaced.
    """
    # Define a regex pattern to match consecutive lines starting with 'import'
    # The (?m) flag allows ^ to match the start of each line
    pattern = r'(?m)^(import\s+.*\n)+'
    # Replace matched groups with '...\n'
    modified_content = re.sub(pattern, '...\n', content)
    return modified_content

def main():
    aggregated_string = aggregate_files(base_path, file_paths)
    
    # Replace groups of import lines with '...'
    aggregated_string = replace_import_groups(aggregated_string)
    
    # Define the output path relative to the base_path or absolute
    output_path = os.path.join(base_path, 'aggregated_contents.txt')
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