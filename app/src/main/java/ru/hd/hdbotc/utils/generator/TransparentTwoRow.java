package ru.hd.hdbotc.utils.generator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransparentTwoRow {

    public Bitmap generateImageFromJson(Context context, JSONArray script, Typeface typefaceR, Typeface typefaceB) {
        try {
            // Размеры страницы А4 (300 DPI)
            int pageWidth = 2480; // Ширина страницы А4
            int pageHeight = 3508; // Высота страницы А4

            // Создаем холст
            Bitmap bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);


            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(100);
            paint.setTypeface(typefaceB);

            // Извлекаем метаданные из первого элемента массива
            JSONObject metaData = script.optJSONObject(0); // Первый элемент — это метаданные
            if (metaData == null) {
                throw new JSONException("Meta data not found in the script.");
            }

            String scriptName = metaData.optString("name", "Unknown Script");
            String author = metaData.optString("author", "Unknown Author");
            String title = scriptName + " by " + author;
            canvas.drawText(title, 100, 100, paint);

            JSONArray charactersData = readRolesFromAssets(context);

            // Разделяем роли по командам
            Map<String, List<String>> teams = new HashMap<>();
            teams.put("townsfolk", new ArrayList<>());
            teams.put("outsider", new ArrayList<>());
            teams.put("minion", new ArrayList<>());
            teams.put("demon", new ArrayList<>());
            teams.put("fabled", new ArrayList<>());

            for (int i = 1; i < script.length(); i++) { // Начинаем с индекса 1 (пропускаем метаданные)
                String characterId = script.optString(i);
                JSONObject character = findCharacterById(characterId, charactersData);
                if (character != null) {
                    String team = character.optString("team", "unknown");
                    if (teams.containsKey(team)) {
                        teams.get(team).add(characterId);
                    }
                }
            }

            // Координаты для размещения блоков
            int currentY = 200;
            String[] teamNames = {"townsfolk", "outsider", "minion", "demon", "fabled"};
            String[] teamNamesRu = {"горожане", "изгои", "приспешники", "демоны", "мифы"};

            for (int j = 0; j < teamNames.length; j++) {
                // Список персонажей в этой команде
                List<String> characterIds = teams.get(teamNames[j]);
                if (characterIds == null || characterIds.isEmpty()) {
                    currentY += 100; // Отступ, если блок пустой
                    continue;
                }
                // Заголовок блока
                paint.setTextSize(60);
                canvas.drawText(teamNamesRu[j].toUpperCase(), 100, currentY + 60, paint);
                currentY += 100;

                int rowHeight = 0; // Высота текущего ряда
                int x = 50; // Начальная координата X для карточки

                for (int i = 0; i < characterIds.size(); i++) {
                    String characterId = characterIds.get(i);
                    JSONObject character = findCharacterById(characterId, charactersData);
                    if (character != null) {
                        Bitmap card = createCharacterCard(character, context, typefaceR, typefaceB);

                        // Если карточка не помещается в текущий ряд, переходим на новый
                        if (x + card.getWidth() > pageWidth) {
                            currentY += rowHeight + 10; // Добавляем высоту ряда и маленький отступ
                            x = 50; // Сбрасываем X для нового ряда
                            rowHeight = 0; // Сбрасываем высоту ряда
                        }

                        // Рисуем карточку
                        canvas.drawBitmap(card, x, currentY, null);

                        // Обновляем координаты и высоту ряда
                        x += card.getWidth(); // Добавляем ширину карточки и маленький отступ
                        rowHeight = Math.max(rowHeight, card.getHeight()); // Высота ряда равна максимальной высоте карточки
                    }
                }

                // Переход на новую строку после завершения ряда
                currentY += rowHeight + 20;

            }

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null; // В случае ошибки возвращаем null
        }
    }

    private JSONObject findCharacterById(String id, JSONArray charactersData) throws Exception {
        for (int i = 0; i < charactersData.length(); i++) {
            JSONObject character = charactersData.getJSONObject(i);
            if (character.getString("id").equals(id)) {
                return character;
            }
        }
        return null;
    }

    private Bitmap createCharacterCard(JSONObject character, Context context, Typeface typefaceR, Typeface typefaceB) throws Exception {
        // Размеры шрифтов
        int nameTextSize = 40; // Размер шрифта для названия
        int abilityTextSize = 30; // Размер шрифта для описания

        // Создаем Paint для текста
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        // Загружаем иконку персонажа
        String iconPath = "icons/" + character.getString("id") + ".png";
        Bitmap icon = null;
        try {
            icon = BitmapFactory.decodeStream(context.getAssets().open(iconPath));
        } catch (Exception ignored) {
        }

        // Размеры иконки (высота как три строки текста)
        int iconSize = 200; // Примерно высота трех строк текста
        Bitmap scaledIcon = null;
        if (icon != null) {
            // Масштабируем иконку до нужного размера
            scaledIcon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, true);
        }

        String ability = character.getString("ability");
        paint.setTextSize(abilityTextSize);
        paint.setTypeface(typefaceR);
        String[] abilityLines = splitTextIntoLines(ability, paint, 990);

        int cardWidth = 1200;
        int cardHeight = nameTextSize + Math.max(3, abilityLines.length) * (abilityTextSize + 20);

        // карточка
        Bitmap card = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888);
        Canvas cardCanvas = new Canvas(card);

        // иконка
        if (scaledIcon != null) {
            int iconY = (cardHeight - iconSize) / 2; // Центрируем иконку по вертикали относительно всей карточки
            cardCanvas.drawBitmap(scaledIcon, 0, iconY, null); // Отступ слева
        }

        // название роли
        String name = character.getString("name");
        paint.setTextSize(nameTextSize);
        paint.setTypeface(typefaceB);
        int nameX = 210; // Отступ после иконки
        int nameY = (cardHeight / 2) - (nameTextSize + 3 * abilityTextSize + 20) / 2 + nameTextSize / 2;

        cardCanvas.drawText(name, nameX, nameY, paint);

        // описание
        paint.setTextSize(abilityTextSize);
        paint.setTypeface(typefaceR);

        int abilityX = 210; // Отступ после иконки
        int abilityY = nameY + nameTextSize + 5; // После названия с отступом

        for (int i = 0; i < abilityLines.length; i++) {
            cardCanvas.drawText(abilityLines[i], abilityX, abilityY + i * (abilityTextSize + 10), paint); // 10px между строками
        }

        return card;
    }

    // Вспомогательный метод для разбиения текста на строки
    private String[] splitTextIntoLines(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine + (currentLine.length() > 0 ? " " : "") + word;
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append((currentLine.length() > 0 ? " " : "")).append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    private JSONArray readRolesFromAssets(Context context) throws Exception {
        InputStream inputStream = context.getAssets().open("roles_ru.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return new JSONArray(stringBuilder.toString());
    }
}
