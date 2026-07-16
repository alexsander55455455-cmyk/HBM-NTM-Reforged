<h1 align="center">HBM NTM Reforged</h1>

<p align="center">
  <a href="README.md">English</a> /
  <a href="README_ru.md">Русский</a> /
  <a href="README_zh.md">简体中文</a> /
  <a href="README_kr.md">한국어</a> /
  <a href="README_ua.md">Українська</a>
</p>

<p align="center">
  <a href="https://discord.gg/BgrqdWEK">
    <img src="https://img.shields.io/discord/901451468282941470?color=5865f2&label=Discord&style=flat&logo=discord&logoColor=white" alt="Discord">
  </a>
  <a href="https://modrinth.com/mod/hbm-ntm-reforged">
    <img src="https://img.shields.io/badge/Modrinth-HBM%20NTM%20Reforged-00AF5C?style=flat&logo=modrinth&logoColor=white" alt="Modrinth">
  </a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/hbm-ntm-reforged">
    <img src="https://img.shields.io/badge/CurseForge-HBM%20NTM%20Reforged-f16436?style=flat&logo=curseforge&logoColor=white" alt="CurseForge">
  </a>
</p>

HBM NTM Reforged — это порт оригинального HBM's Nuclear Tech Mod на Minecraft Forge 1.12.2.

Главная идея проекта простая: перенести оригинальный мод на 1.12.2 и совместить его с контентом из Extended Edition, чтобы игроки получили максимально полный опыт в одном месте. Мне хотелось взять то, что нравилось в Extended Edition, то, чего не хватало из оригинала, и космос — всё в одном моде, без отдельных форков и сборок.

Именно к этому стремится Reforged. Плюс я добавляю свои собственные вещи, которые лично хочу видеть в моде и которых нет в других версиях.

## Discord

Присоединяйтесь к Discord-сообществу для баг-репортов, предложений, новостей разработки, обсуждения modded Minecraft и планов публичного сервера.

[Join the Discord](https://discord.gg/BgrqdWEK)

## Зависимости

### Обязательные

- [MixinBooter](https://modrinth.com/mod/mixinbooter) — обязательная библиотека для совместимости модов на Minecraft 1.12.2.
- [ConnectedTexturesMod / CTM](https://www.curseforge.com/minecraft/mc-mods/ctm/files/2915363) — нужен для connected texture/model support.

## Тестовые сборки (для игроков)

Каждый push в `main` и каждый pull request собирает мод в **GitHub Actions** и выкладывает jar.

1. Открой [Actions](https://github.com/alexsander55455455-cmyk/HBM-NTM-Reforged/actions/workflows/build-test.yml)
2. Выбери успешный run (зелёная галочка)
3. Внизу **Artifacts** → скачай `HBM-NTM-Reforged-testbuild-…`
4. Распакуй zip → jar в папку `mods` (старые HBM Reforged jar убери)

Артефакты живут **30 дней**. Стабильные релизы — на CurseForge / Modrinth.

Ручной запуск: **Actions → Test Build → Run workflow**.

## Сообщить об ошибке

Нашли краш, сломанный рецепт или визуальный баг?

[Создайте Issue на GitHub](https://github.com/alexsander55455455-cmyk/HBM-NTM-Reforged/issues/new?template=bug_report.yml)

Укажите версию мода, Forge, шаги воспроизведения и по возможности `latest.log` или crash-report. Если тестировали свежий фикс — напишите, что брали **test build из Actions**, а не старый релиз.

## CurseForge

https://www.curseforge.com/minecraft/mc-mods/hbm-ntm-reforged

## Credits

Проект вдохновлён работой HBM NTM сообщества и людьми, стоящими за этими модами. Спасибо всем, кто создавал, поддерживал и вносил вклад.

### Основа

- [HBM's Nuclear Tech Mod](https://www.curseforge.com/minecraft/mc-mods/hbms-nuclear-tech-mod) — оригинальный мод и главная база Reforged

### Также вдохновлено

- [HBM's Nuclear Tech Mod Extended Edition](https://www.curseforge.com/minecraft/mc-mods/hbms-nuclear-tech-mod-extended-edition)
- [HBM Nuclear Tech Mod Community Edition](https://www.curseforge.com/minecraft/mc-mods/hbm-nuclear-tech-mod-community-edition)
- [HBM's NTM CE Space](https://www.curseforge.com/minecraft/mc-mods/hbms-ntm-ce-space)

Оригинальный HBM's Nuclear Tech Mod принадлежит его оригинальным создателям.

## Лицензия

Этот проект использует GNU Lesser General Public License v3.0 там, где это применимо.

Подробности смотрите в `LICENSE` и `LICENSE.LESSER`.